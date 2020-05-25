package org.kin.scheduler.core.master;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.actor.PinnedThreadSafeHandler;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.domain.WorkerRes;
import org.kin.scheduler.core.driver.transport.ApplicationRegisterInfo;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.log.StaticLogger;
import org.kin.scheduler.core.master.domain.ApplicationContext;
import org.kin.scheduler.core.master.domain.ExecutorRes;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategies;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;
import org.kin.scheduler.core.master.transport.ApplicationRegisterResponse;
import org.kin.scheduler.core.master.transport.WorkerHeartbeatResp;
import org.kin.scheduler.core.master.transport.WorkerRegisterResult;
import org.kin.scheduler.core.master.transport.WorkerUnregisterResult;
import org.kin.scheduler.core.worker.transport.WorkerHeartbeat;
import org.kin.scheduler.core.worker.transport.WorkerInfo;
import org.kin.scheduler.core.worker.transport.WorkerRegisterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class Master extends AbstractService implements MasterBackend, DriverMasterBackend {
    private static final Logger log = LoggerFactory.getLogger(Master.class);
    private String masterBackendHost;
    /** master rpc端口 */
    private int masterBackendPort;
    //-------------------------------------------------------------------------------------------------
    /** 已注册的worker */
    private Map<String, WorkerContext> workers = new ConcurrentHashMap<>();
    private ServiceConfig masterBackendServiceConfig;
    private ServiceConfig driverMasterBackendServiceConfig;
    private Map<String, ApplicationContext> applicationContexts = new ConcurrentHashMap<>();
    //心跳时间(秒)
    private int heartbeatTime = 3;
    //心跳检测间隔(秒)
    private int heartbeatCheckInterval = heartbeatTime + 2;
    private PinnedThreadSafeHandler<?> threadSafeHandler =
            new PinnedThreadSafeHandler<>(ExecutionContext.fix(1, "Master", 1, "Master-schedule"));

    public Master(String masterBackendHost, int masterBackendPort, String logPath) {
        super("Master");
        this.masterBackendHost = masterBackendHost;
        this.masterBackendPort = masterBackendPort;
        StaticLogger.init(logPath);
    }

    //-------------------------------------------------------------------------------------------------

    @Override
    public void init() {
        super.init();
        masterBackendServiceConfig = Services.service(this, MasterBackend.class)
                .appName(getName())
                .bind(masterBackendHost, masterBackendPort)
                .actorLike();
        try {
            masterBackendServiceConfig.export();
        } catch (Exception e) {
            StaticLogger.log.error(e.getMessage(), e);
        }

        driverMasterBackendServiceConfig = Services.service(this, DriverMasterBackend.class)
                .appName(getName().concat("-ClientBackend"))
                .bind(masterBackendHost, masterBackendPort)
                .actorLike();
        try {
            driverMasterBackendServiceConfig.export();
        } catch (Exception e) {
            StaticLogger.log.error(e.getMessage(), e);
        }
    }

    @Override
    public void start() {
        super.start();

        threadSafeHandler.scheduleAtFixedRate(ts -> checkHeartbeatTimeout(), heartbeatCheckInterval, heartbeatCheckInterval, TimeUnit.SECONDS);

        JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, this::stop);
    }

    @Override
    public void stop() {
        super.stop();
        threadSafeHandler.stop();
        masterBackendServiceConfig.disable();
        driverMasterBackendServiceConfig.disable();
        for (WorkerContext worker : workers.values()) {
            worker.stop();
        }
        workers.clear();
    }

    //-------------------------------------------------------------------------------------------------

    @Override
    public WorkerRegisterResult registerWorker(WorkerRegisterInfo registerInfo) {
        WorkerInfo workerInfo = registerInfo.getWorkerInfo();
        if (workerInfo != null) {
            if (!isInState(State.STARTED)) {
                return WorkerRegisterResult.failure("master not started");
            }

            WorkerContext worker = workers.get(workerInfo.getWorkerId());
            if (Objects.isNull(worker)) {
                worker = new WorkerContext(workerInfo);
                worker.init();
                worker.start();
                workers.put(workerInfo.getWorkerId(), worker);

                return WorkerRegisterResult.success();
            } else {
                return WorkerRegisterResult.failure(String.format("worker(workerId=%s) has registered", workerInfo.getWorkerId()));
            }
        } else {
            return WorkerRegisterResult.failure("worker(workerId=null) register info error");
        }
    }

    @Override
    public WorkerUnregisterResult unregisterWorker(String workerId) {
        if (!isInState(State.STARTED)) {
            return WorkerUnregisterResult.failure("master not started");
        }
        if (StringUtils.isNotBlank(workerId)) {
            if (workers.containsKey(workerId)) {
                WorkerContext worker = workers.remove(workerId);
                worker.stop();

                //广播Driver更新Executor资源
                executorStateChanged(workerId);

                return WorkerUnregisterResult.success();
            } else {
                return WorkerUnregisterResult.failure(String.format("worker(workerId=%s) has not registered", workerId));
            }
        } else {
            return WorkerUnregisterResult.failure(String.format("workerId(%s) error", workerId));
        }
    }

    @Override
    public WorkerHeartbeatResp workerHeartbeat(WorkerHeartbeat heartbeat) {
        if (isInState(State.STARTED)) {
            String hearbeatWorkerId = heartbeat.getWorkerId();
            WorkerContext workerContext = workers.get(hearbeatWorkerId);
            if (Objects.nonNull(workerContext)) {
                workerContext.setLastHeartbeatTime(System.currentTimeMillis());
            } else {
                //发现心跳worker还没注册, 通知其注册
                return WorkerHeartbeatResp.RECONNECT;
            }
        }

        return WorkerHeartbeatResp.EMPTY;
    }

    @Override
    public void executorStateChanged(ExecutorStateChanged executorState) {
        //TODO 恢复已使用资源
        //TODO 移除状态
    }

    private void executorStateChanged(String unAvailableWorkerId) {
        for (ApplicationContext driver : applicationContexts.values()) {
            //无用Executor
            List<String> unAvailableExecutorIds =
                    driver.getUsedExecutorReses().stream()
                            .filter(er -> er.getWorkerRes().getWorkerId().equals(unAvailableWorkerId))
                            .map(ExecutorRes::getExecutorId)
                            .collect(Collectors.toList());
            driver.executorStatusChange(Collections.emptyList(), unAvailableExecutorIds);

            scheduleResource(driver);
        }
    }

    //-------------------------------------------------------------------------------------------------

    @Override
    public ApplicationRegisterResponse registerApplication(ApplicationRegisterInfo request) {
        if (!isInState(State.STARTED)) {
            return ApplicationRegisterResponse.failure("master not started");
        }

        String appName = request.getAppName();
        if (!applicationContexts.containsKey(appName)) {
            return ApplicationRegisterResponse.failure(String.format("application '%s' has registered", appName));
        }

        AllocateStrategy allocateStrategy = AllocateStrategies.getByName(request.getAllocateStrategy());
        if (Objects.isNull(allocateStrategy)) {
            return ApplicationRegisterResponse.failure(String.format("unknown allocate strategy type >>>> %s", request.getAllocateStrategy()));
        }

        ApplicationContext applicationContext = new ApplicationContext(appName, allocateStrategy, request.getExecutorDriverBackendAddress(), request.getMasterDriverBackendAddress());
        applicationContext.init();
        applicationContexts.put(appName, applicationContext);
        return ApplicationRegisterResponse.success();
    }

    @Override
    public void scheduleResource(String appName) {
        ApplicationContext driver = applicationContexts.get(appName);
        if (Objects.nonNull(driver)) {
            scheduleResource(driver);
        }
    }

    private void scheduleResource(ApplicationContext driver) {
        AllocateStrategy allocateStrategy = driver.getAllocateStrategy();
        //寻找可用executor
        List<WorkerRes> usedWorkerReses = allocateStrategy.allocate(workers.values(), driver.getUsedExecutorReses());
        if (CollectionUtils.isNonEmpty(usedWorkerReses)) {
            List<ExecutorRes> executorReses = new ArrayList<>();
            for (WorkerRes usedWorkerRes : usedWorkerReses) {
                WorkerContext worker = workers.get(usedWorkerRes.getWorkerId());
                //TODO 暂时workerId=ExecutorId
                ExecutorRes executorRes = new ExecutorRes(worker.getWorkerInfo().getWorkerId(), usedWorkerRes);
                executorReses.add(executorRes);
            }
            driver.executorStatusChange(executorReses, Collections.emptyList());
        }
    }

    @Override
    public void applicationEnd(String appName) {
        if (isInState(State.STARTED)) {
            ApplicationContext applicationContext = applicationContexts.remove(appName);
            if (Objects.nonNull(applicationContext)) {
                applicationContext.stop();
                StaticLogger.log.error("applicaton '{}' shutdown", applicationContext.getAppName());
            }
        }
    }

    private void checkHeartbeatTimeout() {
        try {
            long sleepTime = heartbeatCheckInterval - TimeUtils.timestamp() % heartbeatCheckInterval;
            if (sleepTime > 0 && sleepTime < heartbeatCheckInterval) {
                TimeUnit.SECONDS.sleep(sleepTime);
            }
        } catch (InterruptedException e) {

        }

        HashSet<String> registeredWorkerIds = new HashSet<>(workers.keySet());
        for (String registeredWorkerId : registeredWorkerIds) {
            WorkerContext workerContext = workers.get(registeredWorkerId);
            if (Objects.nonNull(workerContext) &&
                    workerContext.getLastHeartbeatTime() - System.currentTimeMillis() > TimeUnit.SECONDS.toMillis(heartbeatTime)) {
                //定时检测心跳超时, 并移除超时worker
                unregisterWorker(workerContext.getWorkerInfo().getWorkerId());
            }
        }
    }
}
