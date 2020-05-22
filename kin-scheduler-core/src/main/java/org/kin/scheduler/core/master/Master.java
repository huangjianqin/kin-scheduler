package org.kin.scheduler.core.master;

import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.domain.WorkerRes;
import org.kin.scheduler.core.driver.Job;
import org.kin.scheduler.core.log.StaticLogger;
import org.kin.scheduler.core.master.domain.ExecutorRes;
import org.kin.scheduler.core.master.domain.JobContext;
import org.kin.scheduler.core.master.domain.WorkerContext;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategies;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;
import org.kin.scheduler.core.master.transport.*;
import org.kin.scheduler.core.transport.RPCResult;
import org.kin.scheduler.core.worker.transport.ExecutorLaunchResult;
import org.kin.scheduler.core.worker.transport.WorkerInfo;
import org.kin.scheduler.core.worker.transport.WorkerRegisterInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class Master extends AbstractService implements MasterBackend, DriverMasterBackend {
    private String masterBackendHost;
    /** master rpc端口 */
    private int masterBackendPort;
    //-------------------------------------------------------------------------------------------------
    /** 已注册的worker */
    private Map<String, WorkerContext> workers = new ConcurrentHashMap<>();
    private ServiceConfig masterBackendServiceConfig;
    private ServiceConfig driverMasterBackendServiceConfig;
    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private Map<String, JobContext> jobContexts = new HashMap<>();

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
    }

    @Override
    public void stop() {
        super.stop();
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
        if (!isInState(State.STARTED)) {
            return WorkerRegisterResult.failure("master not started");
        }
        WorkerInfo workerInfo = registerInfo.getWorkerInfo();
        if (workerInfo != null) {
            if (!workers.containsKey(workerInfo.getWorkerId())) {
                WorkerContext worker = new WorkerContext(workerInfo);
                worker.init();
                worker.start();
                workers.put(workerInfo.getWorkerId(), worker);

                return WorkerRegisterResult.success();
            } else {
                return WorkerRegisterResult.failure(String.format("worker(workerId=%s) has registered", workerInfo.getWorkerId()));
            }
        } else {
            return WorkerRegisterResult.failure(String.format("worker(workerId=null) register info error"));
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
                executorStatusChange(workerId);

                return WorkerUnregisterResult.success();
            } else {
                return WorkerUnregisterResult.failure(String.format("worker(workerId=%s) has not registered", workerId));
            }
        } else {
            return WorkerUnregisterResult.failure(String.format("workerId(%s) error", workerId));
        }
    }

    private void executorStatusChange(String unAvailableWorkerId) {
        for (JobContext jobContext : jobContexts.values()) {
            //无用Executor
            List<String> unAvailableExecutorIds =
                    jobContext.getUsedExecutorReses().stream()
                            .filter(er -> er.getWorkerRes().getWorkerId().equals(unAvailableWorkerId))
                            .map(ExecutorRes::getExecutorId)
                            .collect(Collectors.toList());
            jobContext.executorStatusChange(unAvailableExecutorIds);

            //过滤掉无用Executor
            List<ExecutorRes> filterUsedExecutorReses =
                    jobContext.getUsedExecutorReses().stream()
                            .filter(er -> !er.getWorkerRes().getWorkerId().equals(unAvailableWorkerId))
                            .collect(Collectors.toList());
            List<WorkerRes> newUseWorkerReses = jobContext.getAllocateStrategy().allocate(null, workers.values(), filterUsedExecutorReses);
            if (CollectionUtils.isNonEmpty(newUseWorkerReses)) {
                //需要为已启动的job分配新资源
                List<ExecutorRes> newUseExecutorReses = new ArrayList<>(newUseWorkerReses.size());
                for (WorkerRes useWorkerRes : newUseWorkerReses) {
                    WorkerContext worker = workers.get(useWorkerRes.getWorkerId());
                    //启动executor
                    ExecutorLaunchInfo launchInfo = new ExecutorLaunchInfo(jobContext.getExecutorDriverBackendAddress());
                    ExecutorLaunchResult launchResult = worker.launchExecutor(launchInfo);
                    ExecutorRes executorRes = new ExecutorRes(launchResult.getExecutorId(), useWorkerRes);
                    newUseExecutorReses.add(executorRes);
                }

                filterUsedExecutorReses.addAll(newUseExecutorReses);
            }
            jobContext.setUsedExecutorReses(filterUsedExecutorReses);
        }
    }

    //-------------------------------------------------------------------------------------------------

    @Override
    public SubmitJobResponse submitJob(SubmitJobRequest request) {
        if (!isInState(State.STARTED)) {
            return SubmitJobResponse.failure("master not started");
        }

        AllocateStrategy allocateStrategy = AllocateStrategies.getByName(request.getAllocateStrategy());
        if (Objects.isNull(allocateStrategy)) {
            return SubmitJobResponse.failure(String.format("unknown allocate strategy type >>>> %s", request.getAllocateStrategy()));
        }

        //寻找可用executor
        List<WorkerRes> usedWorkerReses = allocateStrategy.allocate(request, workers.values(), Collections.emptyList());
        if (CollectionUtils.isNonEmpty(usedWorkerReses)) {
            List<ExecutorRes> executorReses = new ArrayList<>(usedWorkerReses.size());
            for (WorkerRes useWorkerRes : usedWorkerReses) {
                WorkerContext worker = workers.get(useWorkerRes.getWorkerId());
                //启动executor
                ExecutorLaunchInfo launchInfo = new ExecutorLaunchInfo(request.getExecutorDriverBackendAddress());
                ExecutorLaunchResult launchResult = worker.launchExecutor(launchInfo);
                ExecutorRes executorRes = new ExecutorRes(launchResult.getExecutorId(), useWorkerRes);
                executorReses.add(executorRes);
            }

            String appName = request.getAppName();
            String jobId = "app-".concat(appName.concat("-Job".concat(dateFormat.format(new Date()))));

            JobContext jobContext = new JobContext(jobId, allocateStrategy, executorReses, request.getExecutorDriverBackendAddress(), request.getMasterDriverBackendAddress());
            jobContext.init();
            jobContexts.put(jobId, jobContext);
            return SubmitJobResponse.success(new Job(jobId));
        } else {
            return SubmitJobResponse.failure("cannot assign executor, due to not enough resources");
        }
    }

    @Override
    public void jonFinish(String jobId) {
        if (isInState(State.STARTED)) {
            JobContext jobContext = jobContexts.remove(jobId);
            if (Objects.nonNull(jobContext)) {
                for (ExecutorRes useExecutorRes : jobContext.getUsedExecutorReses()) {
                    WorkerContext worker = workers.get(useExecutorRes.getWorkerRes().getWorkerId());
                    //关闭executor
                    RPCResult result = worker.shutdownExecutor(useExecutorRes.getExecutorId());
                    if (result.isSuccess()) {
                        //TODO 回收资源
                    } else {
                        StaticLogger.log.error("shutdown executor encounter error >>>> {}", result.getDesc());
                    }
                }
                jobContext.stop();
            }
        }
    }
}
