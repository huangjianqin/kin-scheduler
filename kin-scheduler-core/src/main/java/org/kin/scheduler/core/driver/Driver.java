package org.kin.scheduler.core.driver;

import org.kin.framework.service.AbstractService;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.driver.exception.SubmitJobFailureException;
import org.kin.scheduler.core.driver.impl.JobContext;
import org.kin.scheduler.core.master.DriverMasterBackend;
import org.kin.scheduler.core.master.domain.SubmitJobRequest;
import org.kin.scheduler.core.master.domain.SubmitJobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public abstract class Driver extends AbstractService {
    private ReferenceConfig<DriverMasterBackend> driverMasterBackendReferenceConfig;
    protected DriverMasterBackend driverMasterBackend;
    protected JobContext jobContext;
    protected volatile Job job;

    public Driver(JobContext jobContext) {
        super(jobContext.getAppName());
        this.jobContext = jobContext;
    }

    @Override
    public void init() {
        super.init();
        driverMasterBackendReferenceConfig = References.reference(DriverMasterBackend.class)
                .appName(getName())
                .urls(jobContext.getMasterAddress());
        driverMasterBackend = driverMasterBackendReferenceConfig.get();
    }

    @Override
    public void start() {
        //提交job
        super.start();
        try {
            SubmitJobResponse response = driverMasterBackend.submitJob(SubmitJobRequest.create(jobContext.getAppName(), jobContext.getParallelism()));
            if (Objects.nonNull(response)) {
                if (response.isSuccess()) {
                    job = response.getJob();
                } else {
                    throw new SubmitJobFailureException(response.getDesc());
                }
            } else {
                throw new SubmitJobFailureException("master no response");
            }
        } catch (Exception e) {
            close();
            throw new SubmitJobFailureException(e.getMessage());
        }
    }

    @Override
    public void close() {
        super.close();
        if (Objects.nonNull(driverMasterBackend) && Objects.nonNull(job)) {
            driverMasterBackend.jonFinish(job.getJobId());
        }
        if (Objects.nonNull(driverMasterBackendReferenceConfig)) {
            driverMasterBackendReferenceConfig.disable();
        }
    }
}
