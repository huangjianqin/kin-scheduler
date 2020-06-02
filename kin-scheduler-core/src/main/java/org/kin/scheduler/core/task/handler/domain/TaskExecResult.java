package org.kin.scheduler.core.task.handler.domain;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class TaskExecResult implements Serializable {
    private static final long serialVersionUID = 2270632692743128073L;

    protected boolean success;
    protected String reason;

    public TaskExecResult() {
    }

    public TaskExecResult(boolean success) {
        this.success = success;
    }

    //----------------------------------------------------------------------------------------
    public static TaskExecResult success() {
        return new TaskExecResult(true);
    }

    public static TaskExecResult failure() {
        return new TaskExecResult(false);
    }

    //setter && getter
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}