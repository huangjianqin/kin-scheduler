package org.kin.scheduler.core.executor.domain;

import org.kin.scheduler.core.domain.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-03-10
 * <p>
 * task 提交结果
 */
public class TaskSubmitResult extends RPCResult {
    private String taskId;
    private String logPath;

    public TaskSubmitResult() {
    }

    public TaskSubmitResult(boolean success, String desc) {
        super(success, desc);
    }

    public TaskSubmitResult(boolean success, String desc, String taskId, String logPath) {
        super(success, desc);
        this.taskId = taskId;
        this.logPath = logPath;
    }

    public static TaskSubmitResult success(String taskId, String logPath) {
        return new TaskSubmitResult(true, "", taskId, logPath);
    }

    public static TaskSubmitResult failure(String taskId, String desc) {
        return new TaskSubmitResult(false, desc, taskId, null);
    }


    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
}
