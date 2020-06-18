package org.kin.scheduler.core.driver.transport;

import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.scheduler.core.driver.ApplicationDescription;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-11
 */
public class RegisterApplication implements Serializable {
    private static final long serialVersionUID = 8669073559392601217L;

    private ApplicationDescription appDesc;
    private RpcEndpointRef driverRef;

    public RegisterApplication() {
    }

    public static RegisterApplication of(ApplicationDescription appDesc, RpcEndpointRef driverRef) {
        RegisterApplication message = new RegisterApplication();
        message.setAppDesc(appDesc);
        message.setDriverRef(driverRef);
        return message;
    }

    //setter && getter

    public ApplicationDescription getAppDesc() {
        return appDesc;
    }

    public void setAppDesc(ApplicationDescription appDesc) {
        this.appDesc = appDesc;
    }

    public RpcEndpointRef getDriverRef() {
        return driverRef;
    }

    public void setDriverRef(RpcEndpointRef driverRef) {
        this.driverRef = driverRef;
    }
}
