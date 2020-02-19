package org.kin.scheduler.core.master;

import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.driver.Job;
import org.kin.scheduler.core.master.domain.SubmitJobRequest;
import org.kin.scheduler.core.master.domain.SubmitJobResponse;
import org.kin.scheduler.core.utils.LogUtils;
import org.kin.scheduler.core.worker.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2020-02-07
 */
public class Master extends AbstractService implements MasterBackend, DriverMasterBackend {
    private Logger log;

    private String masterBackendHost;
    //master rpc端口
    private int masterBackendPort;
    //日志路径
    private String logPath;
    //-------------------------------------------------------------------------------------------------
    //已注册的worker
    private Map<String, WorkerContext> workers = new HashMap<>();
    private ServiceConfig masterBackendServiceConfig;
    private ServiceConfig driverMasterBackendServiceConfig;
    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private Map<String, JobRes> jobUsedRes = new HashMap<>();

    public Master(String masterBackendHost, int masterBackendPort, String logPath) {
        super("Master");
        this.masterBackendHost = masterBackendHost;
        this.masterBackendPort = masterBackendPort;
        this.logPath = logPath;
    }

    //-------------------------------------------------------------------------------------------------
    @Override
    public void init() {
        super.init();
        log = LogUtils.getMasterLogger(logPath, "master");
        masterBackendServiceConfig = Services.service(this, MasterBackend.class)
                .appName(getName())
                .bind(masterBackendHost, masterBackendPort)
                .actorLike();
        try {
            masterBackendServiceConfig.export();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(-1);
        }

        driverMasterBackendServiceConfig = Services.service(this, DriverMasterBackend.class)
                .appName(getName().concat("-ClientBackend"))
                .bind(masterBackendHost, masterBackendPort)
                .actorLike();
        try {
            driverMasterBackendServiceConfig.export();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
            return WorkerRegisterResult.failure(String.format("worker(workerId=%s) register info error", workerInfo.getWorkerId()));
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
    private List<WorkerRes> getAvailableWorkerRes() {
        return workers.values().stream()
                .filter(worker -> worker.getRes().getParallelism() < worker.getWorkerInfo().getMaxParallelism())
                .map(worker -> {
                    WorkerRes res = new WorkerRes(worker.getWorkerInfo().getWorkerId());
                    res.recoverParallelismRes(worker.getWorkerInfo().getMaxParallelism() - worker.getRes().getParallelism());
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Override
    public SubmitJobResponse submitJob(SubmitJobRequest request) {
        if (!isInState(State.STARTED)) {
            return SubmitJobResponse.failure("master not started");
        }
        String appName = request.getAppName();
        String jobId = "app-".concat(appName.concat("-Job".concat(dateFormat.format(new Date()))));
        //寻找可用executor
        List<WorkerRes> availableWorkerReses = getAvailableWorkerRes();
        int needParallelism = request.getParallelism();
        List<WorkerRes> useWorkerReses = new ArrayList<>();
        for (WorkerRes availableWorkerRes : availableWorkerReses) {
            if (needParallelism == 0) {
                break;
            }

            int useParallelism = 0;
            if (needParallelism >= availableWorkerRes.getParallelism()) {
                useParallelism = availableWorkerRes.getParallelism();
            } else {
                useParallelism = needParallelism;
            }
            needParallelism -= useParallelism;
            WorkerRes useWorkerRes = new WorkerRes(availableWorkerRes.getWorkerId());
            useWorkerRes.recoverParallelismRes(useParallelism);
            useWorkerReses.add(useWorkerRes);
        }

        if (needParallelism == 0 && CollectionUtils.isNonEmpty(useWorkerReses)) {
            List<ExecutorRes> executorReses = new ArrayList<>();
            Map<String, String> executorAddresses = new HashMap<>();
            for (WorkerRes useWorkerRes : useWorkerReses) {
                WorkerContext worker = workers.get(useWorkerRes.getWorkerId());
                //启动executor
                ExecutorLaunchInfo launchInfo = new ExecutorLaunchInfo(useWorkerRes.getParallelism());
                ExecutorLaunchResult launchResult = worker.getWorkerBackend().launchExecutor(launchInfo);
                ExecutorRes executorRes = new ExecutorRes(launchResult.getExecutorId(), useWorkerRes);
                executorReses.add(executorRes);
                executorAddresses.put(launchResult.getExecutorId(), launchResult.getAddress());
            }
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
                    worker.getWorkerBackend().shutdownExecutor(useExecutorRes.getExecutorId());
                    worker.getRes().recoverRes(useExecutorRes.getWorkerRes());
                }
            }
        }
    }
}
