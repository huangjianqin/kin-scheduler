package org.kin.scheduler.core.master.domain;

import org.kin.scheduler.core.master.executor.AllocateStrategyType;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public class SubmitJobRequest implements Serializable {
    private String appName;
    private String allocateStrategy;
    private String executorDriverBackendAddress;
    private String masterDriverBackendAddress;

    public SubmitJobRequest() {
    }

    public static SubmitJobRequest create(String appName, AllocateStrategyType allocateStrategy, String executorDriverBackendAddress, String masterDriverBackendAddress) {
        SubmitJobRequest request = new SubmitJobRequest();
        request.setAppName(appName);
        request.setAllocateStrategy(allocateStrategy.name());
        request.setExecutorDriverBackendAddress(executorDriverBackendAddress);
        request.setMasterDriverBackendAddress(masterDriverBackendAddress);
        return request;
    }

    //setter && getter

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAllocateStrategy() {
        return allocateStrategy;
    }

    public void setAllocateStrategy(String allocateStrategy) {
        this.allocateStrategy = allocateStrategy;
    }

    public String getExecutorDriverBackendAddress() {
        return executorDriverBackendAddress;
    }

    public void setExecutorDriverBackendAddress(String executorDriverBackendAddress) {
        this.executorDriverBackendAddress = executorDriverBackendAddress;
    }

    public String getMasterDriverBackendAddress() {
        return masterDriverBackendAddress;
    }

    public void setMasterDriverBackendAddress(String masterDriverBackendAddress) {
        this.masterDriverBackendAddress = masterDriverBackendAddress;
    }
}
