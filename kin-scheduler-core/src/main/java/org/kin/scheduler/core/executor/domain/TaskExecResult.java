package org.kin.scheduler.core.executor.domain;

import org.kin.scheduler.core.domain.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class TaskExecResult extends RPCResult {
    //是否需要重试
    private boolean needRetry;
    //处理器执行返回结果
    private Object execResult;

    private TaskExecResult() {
    }

    private TaskExecResult(boolean success, String desc) {
        this(success, desc, false, null);
    }

    public TaskExecResult(boolean success, String desc, boolean needRetry, Object execResult) {
        super(success, desc);
        this.needRetry = needRetry;
        this.execResult = execResult;
    }

    private TaskExecResult(boolean result, String desc, Object execResult) {
        this(result, desc, false, execResult);
    }

    //-------------------------------------------------------------------------------------------

    public static TaskExecResult success() {
        return success("");
    }

    public static TaskExecResult success(Object execResult) {
        return success("", execResult);
    }

    public static TaskExecResult success(String desc) {
        return new TaskExecResult(true, desc);
    }

    public static TaskExecResult success(String desc, Object execResult) {
        return new TaskExecResult(true, desc, execResult);
    }

    public static TaskExecResult failure(String desc) {
        return new TaskExecResult(false, desc);
    }

    public static TaskExecResult failureWithRetry(String desc) {
        return new TaskExecResult(false, desc, true, null);
    }

    //setter && getter

    public boolean isNeedRetry() {
        return needRetry;
    }

    public void setNeedRetry(boolean needRetry) {
        this.needRetry = needRetry;
    }

    public Object getExecResult() {
        return execResult;
    }

    public void setExecResult(Object execResult) {
        this.execResult = execResult;
    }
}
