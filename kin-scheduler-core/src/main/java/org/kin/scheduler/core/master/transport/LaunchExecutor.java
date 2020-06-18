package org.kin.scheduler.core.master.transport;

import org.kin.kinrpc.message.core.RpcEndpointRef;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class LaunchExecutor implements Serializable {
    private static final long serialVersionUID = 1093338883206802042L;

    private RpcEndpointRef masterRef;
    private String appName;
    private String executorDriverBackendAddress;
    private int cpuCore;

    public LaunchExecutor() {
    }

    public static LaunchExecutor of(RpcEndpointRef masterRef, String appName, String executorDriverBackendAddress, int cpuCore) {
        LaunchExecutor message = new LaunchExecutor();
        message.masterRef = masterRef;
        message.appName = appName;
        message.executorDriverBackendAddress = executorDriverBackendAddress;
        message.cpuCore = cpuCore;
        return message;
    }

    public RpcEndpointRef getMasterRef() {
        return masterRef;
    }

    public void setMasterRef(RpcEndpointRef masterRef) {
        this.masterRef = masterRef;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getExecutorDriverBackendAddress() {
        return executorDriverBackendAddress;
    }

    public void setExecutorDriverBackendAddress(String executorDriverBackendAddress) {
        this.executorDriverBackendAddress = executorDriverBackendAddress;
    }

    public int getCpuCore() {
        return cpuCore;
    }

    public void setCpuCore(int cpuCore) {
        this.cpuCore = cpuCore;
    }
}
