package org.kin.scheduler.core.driver.transport;

import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public class ApplicationRegisterInfo implements Serializable {
    private static final long serialVersionUID = 8669073559392601217L;

    private String appName;
    private String allocateStrategy;
    private String executorDriverBackendAddress;
    private String masterDriverBackendAddress;

    public ApplicationRegisterInfo() {
    }

    public static ApplicationRegisterInfo create(String appName, AllocateStrategyType allocateStrategy, String executorDriverBackendAddress, String masterDriverBackendAddress) {
        ApplicationRegisterInfo request = new ApplicationRegisterInfo();
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
