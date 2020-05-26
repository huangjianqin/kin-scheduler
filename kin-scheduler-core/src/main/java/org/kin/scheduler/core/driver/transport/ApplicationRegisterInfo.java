package org.kin.scheduler.core.driver.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public class ApplicationRegisterInfo implements Serializable {
    private static final long serialVersionUID = 8669073559392601217L;

    private ApplicationDescription appDesc;
    private String executorDriverBackendAddress;
    private String masterDriverBackendAddress;

    public ApplicationRegisterInfo() {
    }

    public static ApplicationRegisterInfo create(ApplicationDescription appDesc, String executorDriverBackendAddress, String masterDriverBackendAddress) {
        ApplicationRegisterInfo request = new ApplicationRegisterInfo();
        request.setAppDesc(appDesc);
        request.setExecutorDriverBackendAddress(executorDriverBackendAddress);
        request.setMasterDriverBackendAddress(masterDriverBackendAddress);
        return request;
    }

    //setter && getter

    public ApplicationDescription getAppDesc() {
        return appDesc;
    }

    public void setAppDesc(ApplicationDescription appDesc) {
        this.appDesc = appDesc;
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
