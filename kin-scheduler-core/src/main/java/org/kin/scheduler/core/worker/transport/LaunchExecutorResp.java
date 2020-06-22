package org.kin.scheduler.core.worker.transport;

import org.kin.scheduler.core.transport.RPCResp;

/**
 * master通知worker启动executor返回消息
 *
 * @author huangjianqin
 * @date 2020-02-08
 */
public class LaunchExecutorResp extends RPCResp {
    private static final long serialVersionUID = -4359993770222676944L;
    /** app name */
    private String appName;
    /** executor id */
    private String executorId;
    /** worker id */
    private String workerId;
    /** executor占用cpu核心数 */
    private int cpuCore;
    /** executor endpoint地址 */
    private String address;

    public static LaunchExecutorResp of(boolean success, String desc, String appName, String executorId, String workerId, int cpuCore, String address) {
        LaunchExecutorResp message = LaunchExecutorResp.of(success, desc);
        message.appName = appName;
        message.executorId = executorId;
        message.workerId = workerId;
        message.cpuCore = cpuCore;
        message.address = address;
        return message;
    }

    public static LaunchExecutorResp success(String appName, String executorId, String workerId, int cpuCore, String address) {
        return LaunchExecutorResp.of(true, "", appName, executorId, workerId, cpuCore, address);
    }

    public static LaunchExecutorResp of(boolean success, String desc) {
        LaunchExecutorResp message = new LaunchExecutorResp();
        message.success = success;
        message.desc = desc;
        return message;
    }

    public static LaunchExecutorResp failure(String desc) {
        return LaunchExecutorResp.of(false, desc);
    }

    //setter && getter

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getExecutorId() {
        return executorId;
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public int getCpuCore() {
        return cpuCore;
    }

    public void setCpuCore(int cpuCore) {
        this.cpuCore = cpuCore;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
