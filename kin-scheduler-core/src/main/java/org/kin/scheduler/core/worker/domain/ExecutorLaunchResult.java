package org.kin.scheduler.core.worker.domain;

import org.kin.scheduler.core.domain.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class ExecutorLaunchResult extends RPCResult {
    private String executorId;
    private String address;

    public ExecutorLaunchResult() {
    }

    public ExecutorLaunchResult(boolean success, String desc, String executorId, String address) {
        super(success, desc);
        this.executorId = executorId;
        this.address = address;
    }

    public ExecutorLaunchResult(boolean success, String desc) {
        super(success, desc);
    }

    //-----------------------------------------------------------------------------------------
    public static ExecutorLaunchResult success(String executorId, String address) {
        return new ExecutorLaunchResult(true, "", executorId, address);
    }

    public static ExecutorLaunchResult failure(String desc) {
        return new ExecutorLaunchResult(false, desc);
    }

    //setter && getter
    public String getExecutorId() {
        return executorId;
    }

    public void setExecutorId(String executorId) {
        this.executorId = executorId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
