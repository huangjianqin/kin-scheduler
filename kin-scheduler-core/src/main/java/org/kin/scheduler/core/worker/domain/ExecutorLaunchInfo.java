package org.kin.scheduler.core.worker.domain;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class ExecutorLaunchInfo implements Serializable {
    private String executorDriverBackendAddress;

    public ExecutorLaunchInfo() {
    }

    public ExecutorLaunchInfo(String executorDriverBackendAddress) {
        this.executorDriverBackendAddress = executorDriverBackendAddress;
    }

    public String getExecutorDriverBackendAddress() {
        return executorDriverBackendAddress;
    }

    public void setExecutorDriverBackendAddress(String executorDriverBackendAddress) {
        this.executorDriverBackendAddress = executorDriverBackendAddress;
    }
}
