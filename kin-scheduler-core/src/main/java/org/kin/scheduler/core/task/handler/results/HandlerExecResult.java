package org.kin.scheduler.core.task.handler.results;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class HandlerExecResult implements Serializable {
    protected boolean success;

    public HandlerExecResult() {
    }

    public HandlerExecResult(boolean success) {
        this.success = success;
    }

    //----------------------------------------------------------------------------------------

    public static HandlerExecResult success() {
        return new HandlerExecResult(true);
    }

    public static HandlerExecResult failure() {
        return new HandlerExecResult(false);
    }

    //setter && getter
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}