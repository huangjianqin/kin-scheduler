package org.kin.scheduler.core.driver;

import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.scheduler.core.driver.exception.SubmitJobFailureException;
import org.kin.scheduler.core.driver.scheduler.TaskScheduler;
import org.kin.scheduler.core.driver.transport.ApplicationRegisterInfo;
import org.kin.scheduler.core.driver.transport.ExecutorRegisterInfo;
import org.kin.scheduler.core.driver.transport.TaskExecResult;
import org.kin.scheduler.core.master.DriverMasterBackend;
import org.kin.scheduler.core.master.transport.ApplicationRegisterResponse;
import org.kin.scheduler.core.transport.RPCResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public abstract class Driver extends AbstractService implements ExecutorDriverBackend, MasterDriverBackend {
    private static final Logger log = LoggerFactory.getLogger(Driver.class);

    private ReferenceConfig<DriverMasterBackend> driverMasterBackendReferenceConfig;
    protected DriverMasterBackend driverMasterBackend;
    private ServiceConfig executorDriverServiceConfig;
    private ServiceConfig masterDriverServiceConfig;
    protected SchedulerContext jobContext;
    protected TaskScheduler taskScheduler;
    protected volatile Job job;

    public Driver(SchedulerContext jobContext, TaskScheduler taskScheduler) {
        super(jobContext.getAppName());
        this.jobContext = jobContext;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void init() {
        super.init();
        driverMasterBackendReferenceConfig = References.reference(DriverMasterBackend.class)
                .appName(getName().concat("-DriverMasterBackendReference"))
                .urls(jobContext.getMasterAddress());
        driverMasterBackend = driverMasterBackendReferenceConfig.get();
        executorDriverServiceConfig = Services.service(this, ExecutorDriverBackend.class)
                .appName(getName().concat("-ExecutorDriverService"))
                .bind(jobContext.getDriverPort())
                .actorLike();
        try {
            executorDriverServiceConfig.export();
        } catch (Exception e) {
            log.error("executor driver service encounter error >>> ", e);
        }

        masterDriverServiceConfig = Services.service(this, MasterDriverBackend.class)
                .appName(getName().concat("-MasterDriverService"))
                .bind(jobContext.getDriverPort())
                .actorLike();
        try {
            masterDriverServiceConfig.export();
        } catch (Exception e) {
            log.error("master driver service encounter error >>> ", e);
        }

        taskScheduler.init();
        taskScheduler.start();
    }

    @Override
    public void start() {
        //提交job
        super.start();
        try {
            ApplicationRegisterResponse response = driverMasterBackend.registerApplication(
                    ApplicationRegisterInfo.create(jobContext.getAppName(), jobContext.getAllocateStrategyType(),
                            NetUtils.getIpPort(jobContext.getDriverPort()), NetUtils.getIpPort(jobContext.getDriverPort())));
            if (Objects.nonNull(response)) {
                if (!response.isSuccess()) {
                    throw new SubmitJobFailureException(response.getDesc());
                }
            } else {
                throw new SubmitJobFailureException("master no response");
            }
        } catch (Exception e) {
            close();
            throw new SubmitJobFailureException(e.getMessage());
        }
        log.info("driver(appName={}, master={}) started", jobContext.getAppName(), jobContext.getMasterAddress());
    }

    @Override
    public void stop() {
        super.stop();
        if (Objects.nonNull(taskScheduler)) {
            taskScheduler.stop();
        }
        if (Objects.nonNull(driverMasterBackend) && Objects.nonNull(job)) {
            driverMasterBackend.applicationEnd(job.getJobId());
        }
        if (Objects.nonNull(driverMasterBackendReferenceConfig)) {
            driverMasterBackendReferenceConfig.disable();
        }
        executorDriverServiceConfig.disable();
        masterDriverServiceConfig.disable();
        log.info("driver(appName={}, master={}) closed", jobContext.getAppName(), jobContext.getMasterAddress());
    }

    @Override
    public RPCResult registerExecutor(ExecutorRegisterInfo executorRegisterInfo) {
        boolean result = taskScheduler.registerExecutor(executorRegisterInfo);
        return result ? RPCResult.success() : RPCResult.failure("");
    }

    @Override
    public void taskFinish(TaskExecResult execResult) {
        taskScheduler.taskFinish(execResult);
    }

    @Override
    public void executorStatusChange(List<String> newExecutorIds, List<String> unavailableExecutorIds) {
        taskScheduler.executorStatusChange(unavailableExecutorIds);
    }
}
