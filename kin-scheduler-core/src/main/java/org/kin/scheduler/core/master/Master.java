package org.kin.scheduler.core.master;

import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.domain.RPCResult;
import org.kin.scheduler.core.driver.Job;
import org.kin.scheduler.core.log.StaticLogger;
import org.kin.scheduler.core.master.domain.SubmitJobRequest;
import org.kin.scheduler.core.master.domain.SubmitJobResponse;
import org.kin.scheduler.core.master.executor.AllocateStrategies;
import org.kin.scheduler.core.master.executor.AllocateStrategy;
import org.kin.scheduler.core.worker.domain.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private Map<String, JobRes> jobUsedRes = new HashMap<>();

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
            System.exit(-1);
        }

        driverMasterBackendServiceConfig = Services.service(this, DriverMasterBackend.class)
                .appName(getName().concat("-ClientBackend"))
                .bind(masterBackendHost, masterBackendPort)
                .actorLike();
        try {
            driverMasterBackendServiceConfig.export();
        } catch (Exception e) {
            StaticLogger.log.error(e.getMessage(), e);
            System.exit(-1);
        }
    }

    @Override
    public void start() {
        super.start();
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }

    @Override
    public void close() {
        super.close();
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

                return WorkerUnregisterResult.success();
            } else {
                return WorkerUnregisterResult.failure(String.format("worker(workerId=%s) has not registered", workerId));
            }
        } else {
            return WorkerUnregisterResult.failure(String.format("workerId(%s) error", workerId));
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
        List<WorkerRes> usedWorkerReses = allocateStrategy.allocate(request, workers.values());
        if (CollectionUtils.isNonEmpty(usedWorkerReses)) {
            List<ExecutorRes> executorReses = new ArrayList<>(usedWorkerReses.size());
            Map<String, String> executorAddresses = new HashMap<>(usedWorkerReses.size());
            for (WorkerRes useWorkerRes : usedWorkerReses) {
                WorkerContext worker = workers.get(useWorkerRes.getWorkerId());
                //启动executor
                ExecutorLaunchInfo launchInfo = new ExecutorLaunchInfo();
                ExecutorLaunchResult launchResult = worker.getWorkerBackend().launchExecutor(launchInfo);
                ExecutorRes executorRes = new ExecutorRes(launchResult.getExecutorId(), useWorkerRes);
                executorReses.add(executorRes);
                executorAddresses.put(launchResult.getExecutorId(), launchResult.getAddress());
            }

            String appName = request.getAppName();
            String jobId = "app-".concat(appName.concat("-Job".concat(dateFormat.format(new Date()))));

            JobRes jobRes = new JobRes(jobId, executorReses);
            jobUsedRes.put(jobId, jobRes);
            return SubmitJobResponse.success(new Job(jobId, executorAddresses));
        } else {
            return SubmitJobResponse.failure("cannot assign executor, due to not enough resources");
        }
    }

    @Override
    public void jonFinish(String jobId) {
        if (isInState(State.STARTED)) {
            JobRes jobRes = jobUsedRes.remove(jobId);
            if (Objects.nonNull(jobRes)) {
                for (ExecutorRes useExecutorRes : jobRes.getUseExecutorReses()) {
                    WorkerContext worker = workers.get(useExecutorRes.getWorkerRes().getWorkerId());
                    //关闭executor
                    RPCResult result = worker.getWorkerBackend().shutdownExecutor(useExecutorRes.getExecutorId());
                    if (result.isSuccess()) {
                        //TODO 回收资源
                    } else {
                        StaticLogger.log.error("shutdown executor encounter error >>>> {}", result.getDesc());
                    }
                }
            }
        }
    }
}
