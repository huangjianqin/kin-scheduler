package org.kin.scheduler.core.executor.domain;

import org.kin.scheduler.core.domain.RPCResult;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class TaskExecResult extends RPCResult {
    /** 是否需要重试 */
    private boolean needRetry;
    /** 处理器执行返回结果 */
    private Serializable execResult;

    private TaskExecResult() {
    }

    private TaskExecResult(boolean success, String desc) {
        this(success, desc, false, null);
    }

    public TaskExecResult(boolean success, String desc, boolean needRetry, Serializable execResult) {
        super(success, desc);
        this.needRetry = needRetry;
        this.execResult = execResult;
    }

    private TaskExecResult(boolean success, String desc, Serializable execResult) {
        this(success, desc, false, execResult);
    }

    //-------------------------------------------------------------------------------------------

    public static TaskExecResult success() {
        return success("");
    }

    public static TaskExecResult success(Serializable execResult) {
        return success("", execResult);
    }

    public static TaskExecResult success(String desc) {
        return new TaskExecResult(true, desc);
    }

    public static TaskExecResult success(String desc, Serializable execResult) {
        return new TaskExecResult(true, desc, execResult);
    }

    public static TaskExecResult failure(String desc) {
        return new TaskExecResult(false, desc);
    }

    public static TaskExecResult failureWithRetry(String desc) {
        return new TaskExecResult(false, desc, true, null);
    }

    //-------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "TaskExecResult{" +
                "needRetry=" + needRetry +
                ", execResult=" + execResult +
                ", success=" + success +
                ", desc='" + desc + '\'' +
                '}';
    }

    //setter && getter

    public boolean isNeedRetry() {
        return needRetry;
    }

    public void setNeedRetry(boolean needRetry) {
        this.needRetry = needRetry;
    }

    public Serializable getExecResult() {
        return execResult;
    }

    public void setExecResult(Serializable execResult) {
        this.execResult = execResult;
    }
}
