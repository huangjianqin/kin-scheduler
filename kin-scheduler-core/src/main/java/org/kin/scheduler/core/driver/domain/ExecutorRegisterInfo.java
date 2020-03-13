package org.kin.scheduler.core.driver.domain;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-03-09
 */
public class ExecutorRegisterInfo implements Serializable {
    private String executorId;
    private String address;

    public ExecutorRegisterInfo() {
    }

    public ExecutorRegisterInfo(String executorId, String address) {
        this.executorId = executorId;
        this.address = address;
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
