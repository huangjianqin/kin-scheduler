package org.kin.scheduler.core.master;

import ch.qos.logback.classic.Logger;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.actor.PinnedThreadSafeHandler;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.domain.WorkerResource;
import org.kin.scheduler.core.driver.ApplicationDescription;
import org.kin.scheduler.core.driver.transport.ApplicationRegisterInfo;
import org.kin.scheduler.core.executor.domain.ExecutorState;
import org.kin.scheduler.core.executor.transport.ExecutorStateChanged;
import org.kin.scheduler.core.log.Loggers;
import org.kin.scheduler.core.master.domain.ApplicationContext;
import org.kin.scheduler.core.master.domain.ExecutorResource;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;
import org.kin.scheduler.core.master.transport.ApplicationRegisterResponse;
import org.kin.scheduler.core.master.transport.ExecutorLaunchInfo;
import org.kin.scheduler.core.master.transport.WorkerHeartbeatResp;
import org.kin.scheduler.core.master.transport.WorkerRegisterResult;
import org.kin.scheduler.core.worker.transport.ExecutorLaunchResult;
import org.kin.scheduler.core.worker.transport.WorkerHeartbeat;
import org.kin.scheduler.core.worker.transport.WorkerInfo;
import org.kin.scheduler.core.worker.transport.WorkerRegisterInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class Master extends AbstractService implements MasterBackend, DriverMasterBackend {
    private Logger log;

    private String masterBackendHost;
    /** master rpc端口 */
    private int masterBackendPort;
    //-------------------------------------------------------------------------------------------------
    /** 已注册的worker */
    private Map<String, WorkerContext> workers = new ConcurrentHashMap<>();
    private ServiceConfig masterBackendServiceConfig;
    private ServiceConfig driverMasterBackendServiceConfig;
    private Map<String, ApplicationContext> applicationContexts = new ConcurrentHashMap<>();
    private List<ApplicationContext> waitingDrivers = new ArrayList<>();
    //心跳时间(秒)
    private int heartbeatTime;
    //心跳检测间隔(秒)
    private int heartbeatCheckInterval = heartbeatTime + 2000;
    private PinnedThreadSafeHandler<?> threadSafeHandler =
            new PinnedThreadSafeHandler<>(ExecutionContext.fix(1, "Master", 1, "Master-schedule"));

    public Master(String masterBackendHost, int masterBackendPort, String logPath, int heartbeatTime) {
        super("master");
        this.masterBackendHost = masterBackendHost;
        this.masterBackendPort = masterBackendPort;
        this.heartbeatTime = heartbeatTime;
        log = Loggers.master(logPath, getName());
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
            log.error(e.getMessage(), e);
        }

        driverMasterBackendServiceConfig = Services.service(this, DriverMasterBackend.class)
                .appName(getName().concat("-ClientBackend"))
                .bind(masterBackendHost, masterBackendPort)
                .actorLike();
        try {
            driverMasterBackendServiceConfig.export();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, this::stop);
    }

    @Override
    public void start() {
        super.start();

        threadSafeHandler.scheduleAtFixedRate(ts -> checkHeartbeatTimeout(), heartbeatCheckInterval, heartbeatCheckInterval, TimeUnit.MILLISECONDS);

        log.info("Master '{}' started", getName());
    }

    @Override
    public void stop() {
        super.stop();
        threadSafeHandler.stop();
        masterBackendServiceConfig.disable();
        driverMasterBackendServiceConfig.disable();
        for (ApplicationContext applicationContext : applicationContexts.values()) {
            applicationContext.stop();
        }
        for (WorkerContext worker : workers.values()) {
            worker.stop();
        }
        workers.clear();

        log.info("Master '{}' stopped", getName());
    }

    //-------------------------------------------------------------------------------------------------

    @Override
    public WorkerRegisterResult registerWorker(WorkerRegisterInfo registerInfo) {
        WorkerInfo workerInfo = registerInfo.getWorkerInfo();
        if (workerInfo != null) {
            if (!isInState(State.STARTED)) {
                return WorkerRegisterResult.failure("master not started");
            }

            String workerId = workerInfo.getWorkerId();
            WorkerContext worker = workers.get(workerId);
            if (Objects.isNull(worker)) {
                worker = new WorkerContext(workerInfo);
                worker.init();
                worker.start();
                workers.put(workerId, worker);

                log.info("worker '{}' registered", workerId);
                return WorkerRegisterResult.success();
            } else {
                return WorkerRegisterResult.failure(String.format("worker(workerId=%s) has registered", workerId));
            }
        } else {
            return WorkerRegisterResult.failure("worker(workerId=null) register info error");
        }
    }

    @Override
    public void unregisterWorker(String workerId) {
        if (isInState(State.STARTED)) {
            if (StringUtils.isNotBlank(workerId)) {
                if (workers.containsKey(workerId)) {
                    WorkerContext worker = workers.remove(workerId);
                    worker.stop();

                    //广播Driver更新Executor资源
                    executorStateChanged(workerId);
                }
            }
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
    public void executorStateChanged(ExecutorStateChanged executorStateChanged) {
        String appName = executorStateChanged.getAppName();
        String executorId = executorStateChanged.getExecutorId();
        ApplicationContext driver = applicationContexts.get(appName);
        if (Objects.nonNull(driver) && driver.containsExecutorResource(executorId)) {
            ExecutorState executorState = executorStateChanged.getState();
            if (!executorState.isFinished()) {
                if (ExecutorState.RUNNING.equals(executorState)) {
                    //已在启动executor时预占用资源
                    driver.executorStatusChange(Collections.singletonList(executorId), Collections.emptyList());
                }
                //TODO 启动状态暂时不处理
            } else {
                //
                ExecutorResource executorResource = driver.removeExecutorResource(executorId);
                if (Objects.nonNull(executorResource)) {
                    WorkerResource workerResource = executorResource.getWorkerResource();
                    WorkerContext workerContext = workers.get(workerResource.getWorkerId());
                    if (Objects.nonNull(workerContext)) {
                        workerContext.getResource().recoverCpuCore(workerResource.getCpuCore());
                    }
                    driver.executorStatusChange(Collections.emptyList(), Collections.singletonList(executorId));
                    tryWaitingResource(driver);
                    scheduleResource();
                }
            }
        }
    }

    private void executorStateChanged(String unAvailableWorkerId) {
        for (ApplicationContext driver : applicationContexts.values()) {
            //无用Executor
            List<String> unAvailableExecutorIds =
                    driver.getUsedExecutorResources().stream()
                            .filter(er -> er.getWorkerResource().getWorkerId().equals(unAvailableWorkerId))
                            .map(ExecutorResource::getExecutorId)
                            .collect(Collectors.toList());
            driver.executorStatusChange(Collections.emptyList(), unAvailableExecutorIds);

            tryWaitingResource(driver);
        }
        scheduleResource();
    }

    //-------------------------------------------------------------------------------------------------

    @Override
    public ApplicationRegisterResponse registerApplication(ApplicationRegisterInfo request) {
        if (!isInState(State.STARTED)) {
            return ApplicationRegisterResponse.failure("master not started");
        }

        ApplicationDescription appDesc = request.getAppDesc();

        String appName = appDesc.getAppName();
        if (applicationContexts.containsKey(appName)) {
            return ApplicationRegisterResponse.failure(String.format("application '%s' has registered", appName));
        }

        if (Objects.isNull(request.getAppDesc().getAllocateStrategy())) {
            return ApplicationRegisterResponse.failure("unknown allocate strategy type");
        }

        ApplicationContext applicationContext = new ApplicationContext(appDesc, request.getExecutorDriverBackendAddress(), request.getMasterDriverBackendAddress());
        applicationContext.init();
        applicationContexts.put(appName, applicationContext);
        log.info("application '{}' registered", appName);
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
        ApplicationDescription appDesc = driver.getAppDesc();
        AllocateStrategy allocateStrategy = appDesc.getAllocateStrategy();
        //寻找可用executor
        //A.判断driver是否可以进行资源分配
        boolean oneExecutorPerWorker = appDesc.isOneExecutorPerWorker();
        int minCoresPerExecutor = appDesc.getMinCoresPerExecutor();

        int cpuCoreLeft = driver.getCpuCoreLeft();
        if (cpuCoreLeft <= 0 || cpuCoreLeft < minCoresPerExecutor) {
            //1.不需要额外资源了
            //2.资源不满足每个Executor最低要求
            return;
        }

        //B.寻找可以分配资源的worker
        List<WorkerContext> registeredWorkerContexts = new ArrayList<>(workers.values());
        registeredWorkerContexts = registeredWorkerContexts.stream()
                //worker资源满足minCoresPerExecutor
                .filter(wc -> wc.getResource().getCpuCore() >= minCoresPerExecutor &&
                        //1.每个worker可以多个executor
                        //2.该worker还未分配executor
                        (!oneExecutorPerWorker || driver.containsWorkerResource(wc.getWorkerInfo().getWorkerId())))
                .collect(Collectors.toList());

        //C.根据资源分配策略获取准备要分配资源的worker
        List<WorkerContext> strategiedWorkerContexts = allocateStrategy.allocate(registeredWorkerContexts);
        if (CollectionUtils.isNonEmpty(strategiedWorkerContexts)) {
            for (WorkerContext availableWorkerContext : strategiedWorkerContexts) {
                //D.executor源分配
                try {
                    cpuCoreLeft = driver.getCpuCoreLeft();
                    if (cpuCoreLeft <= 0) {
                        //不需要额外资源了
                        break;
                    }

                    WorkerInfo availableWorkerInfo = availableWorkerContext.getWorkerInfo();
                    String availableWorkerId = availableWorkerInfo.getWorkerId();

                    //executor需要分配的cpu核心数
                    int minAllocateCpuCore = Math.min(minCoresPerExecutor, cpuCoreLeft);

                    if (minAllocateCpuCore <= 0) {
                        //资源不足
                        continue;
                    }

                    WorkerContext worker = workers.get(availableWorkerId);

                    //启动Executor
                    ExecutorLaunchResult launchResult = worker.launchExecutor(
                            new ExecutorLaunchInfo(appDesc.getAppName(), driver.getExecutorDriverBackendAddress(), minAllocateCpuCore));
                    if (launchResult.isSuccess()) {
                        String executorId = launchResult.getExecutorId();
                        //启动Executor成功
                        //修改worker已使用资源
                        worker.getResource().useCpuCore(minAllocateCpuCore);
                        //修改driver已使用资源
                        driver.useExecutorResource(executorId, new WorkerResource(availableWorkerId, minAllocateCpuCore));
                    }
                } catch (Exception e) {
                    log.error("master '" + getName() + "' allocate executor error >>> ", e);
                }
            }
        }
        //E.如果driver资源还未分配足够, 进入等待队列继续等待足够资源
        tryWaitingResource(driver);
    }

    private void scheduleResource() {
        List<ApplicationContext> waitingDrivers = new ArrayList<>(this.waitingDrivers);
        this.waitingDrivers = new ArrayList<>();
        for (ApplicationContext waitingDriver : waitingDrivers) {
            scheduleResource(waitingDriver);
        }
    }

    private void tryWaitingResource(ApplicationContext driver) {
        //资源分配不足仍然需要在队列等待有足够的资源分配
        if (driver.getCpuCoreLeft() > 0) {
            waitingDrivers.add(driver);
        }
    }

    @Override
    public void applicationEnd(String appName) {
        if (isInState(State.STARTED)) {
            ApplicationContext applicationContext = applicationContexts.remove(appName);
            if (Objects.nonNull(applicationContext)) {
                applicationContext.stop();
                log.error("applicaton '{}' shutdown", applicationContext.getAppDesc());
            }
        }
    }

    private void checkHeartbeatTimeout() {
        try {
            long sleepTime = heartbeatCheckInterval - System.currentTimeMillis() % heartbeatCheckInterval;
            if (sleepTime > 0 && sleepTime < heartbeatCheckInterval) {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            }
        } catch (InterruptedException e) {

        }

        HashSet<String> registeredWorkerIds = new HashSet<>(workers.keySet());
        for (String registeredWorkerId : registeredWorkerIds) {
            WorkerContext workerContext = workers.get(registeredWorkerId);
            if (Objects.nonNull(workerContext) &&
                    workerContext.getLastHeartbeatTime() - System.currentTimeMillis() > TimeUnit.MILLISECONDS.toMillis(heartbeatTime)) {
                //定时检测心跳超时, 并移除超时worker
                unregisterWorker(workerContext.getWorkerInfo().getWorkerId());
            }
        }
    }
}
