package org.kin.scheduler.core.driver.transport;

import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.scheduler.core.driver.ApplicationDescription;

import java.io.Serializable;

/**
 * application 注册消息
 *
 * @author huangjianqin
 * @date 2020-02-11
 */
public class RegisterApplication implements Serializable {
    private static final long serialVersionUID = 8669073559392601217L;
    /** application 描述 */
    private ApplicationDescription appDesc;
    /** scheduler client ref */
    private RpcEndpointRef schedulerRef;

    public RegisterApplication() {
    }

    public static RegisterApplication of(ApplicationDescription appDesc, RpcEndpointRef schedulerRef) {
        RegisterApplication message = new RegisterApplication();
        message.setAppDesc(appDesc);
        message.setSchedulerRef(schedulerRef);
        return message;
    }

    //setter && getter

    public ApplicationDescription getAppDesc() {
        return appDesc;
    }

    public void setAppDesc(ApplicationDescription appDesc) {
        this.appDesc = appDesc;
    }

    public RpcEndpointRef getSchedulerRef() {
        return schedulerRef;
    }

    public void setSchedulerRef(RpcEndpointRef schedulerRef) {
        this.schedulerRef = schedulerRef;
    }
}
