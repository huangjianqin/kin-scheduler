package org.kin.scheduler.core.master.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class ExecutorLaunchInfo implements Serializable {
    private static final long serialVersionUID = 1093338883206802042L;

    private String appName;
    private String executorDriverBackendAddress;
    private int cpuCore;

    public ExecutorLaunchInfo() {
    }

    public ExecutorLaunchInfo(String appName, String executorDriverBackendAddress, int cpuCore) {
        this.appName = appName;
        this.executorDriverBackendAddress = executorDriverBackendAddress;
        this.cpuCore = cpuCore;
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
