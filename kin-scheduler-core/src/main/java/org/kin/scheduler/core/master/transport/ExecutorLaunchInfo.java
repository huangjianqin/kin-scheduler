package org.kin.scheduler.core.master.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class ExecutorLaunchInfo implements Serializable {
    private static final long serialVersionUID = 1093338883206802042L;

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
