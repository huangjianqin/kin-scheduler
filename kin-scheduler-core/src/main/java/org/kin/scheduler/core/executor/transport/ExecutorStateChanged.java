package org.kin.scheduler.core.executor.transport;

import org.kin.scheduler.core.executor.domain.ExecutorState;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-05-25
 */
public class ExecutorStateChanged implements Serializable {
    private static final long serialVersionUID = 8314416045349454746L;

    private String appName;
    private String executorId;
    private ExecutorState state;

    public ExecutorStateChanged() {
    }

    public ExecutorStateChanged(String appName, String executorId, ExecutorState state) {
        this.appName = appName;
        this.executorId = executorId;
        this.state = state;
    }


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

    public ExecutorState getState() {
        return state;
    }

    public void setState(ExecutorState state) {
        this.state = state;
    }
}
