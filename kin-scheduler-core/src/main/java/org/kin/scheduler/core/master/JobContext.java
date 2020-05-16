package org.kin.scheduler.core.master;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.References;
import org.kin.scheduler.core.driver.MasterDriverBackend;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategy;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class JobContext implements MasterDriverBackend {
    private String jobId;
    private AllocateStrategy allocateStrategy;
    private List<ExecutorRes> usedExecutorReses;
    private String executorDriverBackendAddress;
    private String masterDriverBackendAddress;
    private ReferenceConfig<MasterDriverBackend> referenceConfig;
    private MasterDriverBackend masterDriverBackend;

    public JobContext(String jobId, AllocateStrategy allocateStrategy, List<ExecutorRes> usedExecutorReses, String executorDriverBackendAddress, String masterDriverBackendAddress) {
        this.jobId = jobId;
        this.allocateStrategy = allocateStrategy;
        this.usedExecutorReses = usedExecutorReses;
        this.executorDriverBackendAddress = executorDriverBackendAddress;
        this.masterDriverBackendAddress = masterDriverBackendAddress;
    }

    public void init() {
        referenceConfig = References
                .reference(MasterDriverBackend.class)
                .appName("Master".concat("-").concat("MasterDriverBackend"))
                .urls(masterDriverBackendAddress);
        masterDriverBackend = referenceConfig.get();
    }

    public void stop() {
        referenceConfig.disable();
    }

    @Override
    public void executorStatusChange(List<String> unAvailableExecutorIds) {
        masterDriverBackend.executorStatusChange(unAvailableExecutorIds);
    }

    //getter
    public String getJobId() {
        return jobId;
    }

    public AllocateStrategy getAllocateStrategy() {
        return allocateStrategy;
    }

    public String getExecutorDriverBackendAddress() {
        return executorDriverBackendAddress;
    }

    public List<ExecutorRes> getUsedExecutorReses() {
        return usedExecutorReses;
    }

    public void setUsedExecutorReses(List<ExecutorRes> usedExecutorReses) {
        this.usedExecutorReses = usedExecutorReses;
    }
}
