package org.kin.scheduler.core.driver.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public class ExecutorRegisterInfo implements Serializable {
    private String workerId;
    private String executorId;
    private String address;

    public ExecutorRegisterInfo() {
    }

    public ExecutorRegisterInfo(String workerId, String executorId, String address) {
        this.workerId = workerId;
        this.executorId = executorId;
        this.address = address;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

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
