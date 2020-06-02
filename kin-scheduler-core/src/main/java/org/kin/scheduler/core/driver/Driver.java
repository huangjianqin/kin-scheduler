package org.kin.scheduler.core.driver;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.driver.exception.RegisterApplicationFailureException;
import org.kin.scheduler.core.driver.scheduler.TaskContext;
import org.kin.scheduler.core.driver.scheduler.TaskScheduler;
import org.kin.scheduler.core.driver.transport.ApplicationRegisterInfo;
import org.kin.scheduler.core.master.DriverMasterBackend;
import org.kin.scheduler.core.master.transport.ApplicationRegisterResponse;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public abstract class Driver extends AbstractService implements MasterDriverBackend {
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

    public Driver(Application app, TaskScheduler taskScheduler) {
        super(app.getAppName());
        this.app = app;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void init() {
        super.init();
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
    public void start() {
        //注册application
        super.start();
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
    public void stop() {
        super.stop();
        if (Objects.nonNull(taskScheduler)) {
            taskScheduler.stop();
        }
        if (Objects.nonNull(driverMasterBackend)) {
            try {
                driverMasterBackend.applicationEnd(app.getAppName());
            } catch (Exception e) {
                log.error("", e);
            }
            driverMasterBackendReferenceConfig.disable();
        }

        masterDriverServiceConfig.disable();
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

        return driverMasterBackend.readFile(taskContext.getWorkerId(), taskContext.getLogPath(), fromLineNum);
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

        return driverMasterBackend.readFile(taskContext.getWorkerId(), taskContext.getOutputPath(), fromLineNum);
    }
}
