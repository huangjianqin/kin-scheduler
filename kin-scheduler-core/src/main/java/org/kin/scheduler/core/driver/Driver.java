package org.kin.scheduler.core.driver;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.cluster.Clusters;
import org.kin.kinrpc.cluster.exception.CannotFindInvokerException;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.driver.exception.RegisterApplicationFailureException;
import org.kin.scheduler.core.driver.scheduler.TaskContext;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.driver.scheduler.TaskScheduler;
import org.kin.scheduler.core.driver.scheduler.impl.DefaultTaskScheduler;
import org.kin.scheduler.core.driver.transport.ApplicationRegisterInfo;
import org.kin.scheduler.core.master.DriverMasterBackend;
import org.kin.scheduler.core.master.transport.ApplicationRegisterResponse;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class Driver extends AbstractService implements MasterDriverBackend {
    private static final Logger log = LoggerFactory.getLogger(Driver.class);

    /** driver -> master rpc引用 */
    private ReferenceConfig<DriverMasterBackend> driverMasterBackendReferenceConfig;
    protected DriverMasterBackend driverMasterBackend;
    /** master -> driver 服务配置 */
    private ServiceConfig masterDriverServiceConfig;
    /** application配置 */
    protected Application app;
    /** TaskScheduler实现 */
    protected TaskScheduler taskScheduler;

    //---------------------------------------------------------------------------------------------------------------------
    public static Driver a(Application app) {
        return new Driver(app, new DefaultTaskScheduler(app));
    }

    //---------------------------------------------------------------------------------------------------------------------
    public Driver(Application app, TaskScheduler taskScheduler) {
        super(app.getAppName());
        this.app = app;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void serviceInit() {
        driverMasterBackendReferenceConfig = References.reference(DriverMasterBackend.class)
                .appName(getName().concat("-DriverMasterBackendReference"))
                .urls(app.getMasterAddress());
        driverMasterBackend = driverMasterBackendReferenceConfig.get();

        masterDriverServiceConfig = Services.service(this, MasterDriverBackend.class)
                .appName(getName().concat("-MasterDriverService"))
                .bind(app.getDriverPort())
                .actorLike();
        try {
            masterDriverServiceConfig.export();
        } catch (Exception e) {
            log.error("master driver service encounter error >>> ", e);
        }

        taskScheduler.init();
        taskScheduler.start();

        JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, this::stop);
    }

    @Override
    public void serviceStart() {
        //注册applications
        try {
            ApplicationDescription appDesc = new ApplicationDescription();
            appDesc.setAppName(app.getAppName());
            appDesc.setAllocateStrategyType(app.getAllocateStrategyType());
            appDesc.setCpuCoreNum(app.getCpuCoreNum());
            appDesc.setMinCoresPerExecutor(app.getMinCoresPerExecutor());
            appDesc.setOneExecutorPerWorker(app.isOneExecutorPerWorker());

            ApplicationRegisterResponse response = driverMasterBackend.registerApplication(
                    ApplicationRegisterInfo.create(appDesc, NetUtils.getIpPort(app.getDriverPort()), NetUtils.getIpPort(app.getDriverPort())));
            if (Objects.nonNull(response)) {
                if (response.isSuccess()) {
                    //TODO 可以做点事
                    driverMasterBackend.scheduleResource(appDesc.getAppName());
                } else {
                    throw new RegisterApplicationFailureException(response.getDesc());
                }
            } else {
                throw new RegisterApplicationFailureException("master no response");
            }
        } catch (Exception e) {
            close();
            throw new RegisterApplicationFailureException(e.getMessage());
        }
        log.info("driver(appName={}, master={}) started", app.getAppName(), app.getMasterAddress());
    }

    @Override
    public void serviceStop() {
        //先通知master 应用stop
        if (Objects.nonNull(driverMasterBackend)) {
            try {
                driverMasterBackend.applicationEnd(app.getAppName());
            } catch (CannotFindInvokerException e) {

            } catch (Exception e) {
                log.error("", e);
            }
            driverMasterBackendReferenceConfig.disable();
        }
        //再shutdown executor
        //防止无用executor分配, 如果先shutdown executor再通知master 应用stop, master存在再次为该应用分配资源的可能
        if (Objects.nonNull(taskScheduler)) {
            taskScheduler.stop();
        }

        masterDriverServiceConfig.disable();
        Clusters.shutdown();
        TaskExecFuture.CALLBACK_EXECUTORS.shutdownNow();
        log.info("driver(appName={}, master={}) closed", app.getAppName(), app.getMasterAddress());
    }

    @Override
    public final void executorStatusChange(List<String> newExecutorIds, List<String> unavailableExecutorIds) {
        taskScheduler.executorStatusChange(unavailableExecutorIds);
    }

    public final void awaitTermination() {
        taskScheduler.awaitTermination();
    }

    /**
     * 向master请求某worker上的log文件
     *
     * @param taskId      task id
     * @param fromLineNum 开始的行数
     * @return log info
     */
    public final TaskExecFileContent readLog(String taskId, int fromLineNum) {
        TaskContext taskContext = taskScheduler.getTaskInfo(taskId);
        if (Objects.isNull(taskContext)) {
            throw new IllegalStateException(String.format("unknown task(taskid='%s')", taskId));
        }

        return readLog(taskContext.getWorkerId(), taskContext.getLogPath(), fromLineNum);
    }

    public final TaskExecFileContent readLog(String workerId, String logPath, int fromLineNum) {
        return driverMasterBackend.readFile(workerId, logPath, fromLineNum);
    }

    /**
     * 向master请求某worker上的output文件
     *
     * @param taskId      task id
     * @param fromLineNum 开始的行数
     * @return output info
     */
    public final TaskExecFileContent readOutput(String taskId, int fromLineNum) {
        TaskContext taskContext = taskScheduler.getTaskInfo(taskId);
        if (Objects.isNull(taskContext)) {
            throw new IllegalStateException(String.format("unknown task(taskid='%s')", taskId));
        }

        return readOutput(taskContext.getWorkerId(), taskContext.getOutputPath(), fromLineNum);
    }

    public final TaskExecFileContent readOutput(String workerId, String outputPath, int fromLineNum) {
        return driverMasterBackend.readFile(workerId, outputPath, fromLineNum);
    }


    public final <R extends Serializable, PARAM extends Serializable> TaskExecFuture<R> submitTask(TaskDescription<PARAM> taskDescription) {
        return taskScheduler.submitTask(taskDescription);
    }


    public final boolean cancelTask(String taskId) {
        return taskScheduler.cancelTask(taskId);
    }
}
