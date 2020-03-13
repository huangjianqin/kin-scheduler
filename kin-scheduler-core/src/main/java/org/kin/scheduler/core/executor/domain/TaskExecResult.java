package org.kin.scheduler.core.executor.domain;

import org.kin.scheduler.core.domain.RPCResult;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * task执行结果
 */
public class TaskExecResult extends RPCResult {
    private String taskId;
    private String logFileName;
    /** 处理器执行返回结果 */
    private Serializable execResult;

    public TaskExecResult() {
    }

    public TaskExecResult(String taskId, String logFileName, boolean success, String desc, Serializable execResult) {
        super(success, desc);
        this.taskId = taskId;
        this.logFileName = logFileName;
        this.execResult = execResult;
    }

    //-------------------------------------------------------------------------------------------
    public static TaskExecResult success(String taskId, String logFileName, String desc, Serializable execResult) {
        return new TaskExecResult(taskId, logFileName, true, desc, execResult);
    }

    public static TaskExecResult failure(String taskId, String logFileName, String desc) {
        return new TaskExecResult(taskId, logFileName, false, desc, null);
    }

    //-------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "TaskExecResult{" +
                ", execResult=" + execResult +
                ", success=" + success +
                ", desc='" + desc + '\'' +
                '}';
    }

    //setter && getter
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Serializable getExecResult() {
        return execResult;
    }

    public void setExecResult(Serializable execResult) {
        this.execResult = execResult;
    }

    public String getLogFileName() {
        return logFileName;
    }
}
