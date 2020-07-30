package org.kin.scheduler.core.master.transport;

import org.kin.kinrpc.message.core.RpcEndpointRef;

import java.io.Serializable;

/**
 * master通知worker启动executor消息
 *
 * @author huangjianqin
 * @date 2020-02-09
 */
public class LaunchExecutor implements Serializable {
    private static final long serialVersionUID = 1093338883206802042L;
    /** master client ref */
    private RpcEndpointRef masterRef;
    /** application name */
    private String appName;
    /** scheduler endpoint地址 */
    private String executorSchedulerAddress;
    /** executorId */
    private String executorId;
    /** executor所需cpu核心数 */
    private int cpuCore;

    public LaunchExecutor() {
    }

    public static LaunchExecutor of(RpcEndpointRef masterRef, String appName, String executorSchedulerAddress, String executorId, int cpuCore) {
        LaunchExecutor message = new LaunchExecutor();
        message.masterRef = masterRef;
        message.appName = appName;
        message.executorSchedulerAddress = executorSchedulerAddress;
        message.executorId = executorId;
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

    public String getExecutorSchedulerAddress() {
        return executorSchedulerAddress;
    }

    public void setExecutorSchedulerAddress(String executorSchedulerAddress) {
        this.executorSchedulerAddress = executorSchedulerAddress;
    }

    public String getExecutorId() {
        return executorId;
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public int getCpuCore() {
        return cpuCore;
    }

    public void setCpuCore(int cpuCore) {
        this.cpuCore = cpuCore;
    }
}
