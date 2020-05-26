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

    public static ExecutorStateChanged launching(String appName, String executorId) {
        return new ExecutorStateChanged(appName, executorId, ExecutorState.LAUNCHING);
    }

    public static ExecutorStateChanged running(String appName, String executorId) {
        return new ExecutorStateChanged(appName, executorId, ExecutorState.RUNNING);
    }

    public static ExecutorStateChanged fail(String appName, String executorId) {
        return new ExecutorStateChanged(appName, executorId, ExecutorState.FAIL);
    }

    public static ExecutorStateChanged kill(String appName, String executorId) {
        return new ExecutorStateChanged(appName, executorId, ExecutorState.KILLED);
    }

    public static ExecutorStateChanged exit(String appName, String executorId) {
        return new ExecutorStateChanged(appName, executorId, ExecutorState.EXIT);
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
