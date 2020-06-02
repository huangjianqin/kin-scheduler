package org.kin.scheduler.core.driver.transport;

import org.kin.scheduler.core.task.domain.TaskStatus;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * task执行结果
 */
public class TaskStatusChanged implements Serializable {
    private static final long serialVersionUID = -9038880319405781079L;

    private String taskId;
    private TaskStatus status;
    private String logFileName = "";
    /** 处理器执行返回结果 */
    private Serializable execResult;
    private String reason = "";

    public TaskStatusChanged() {
    }

    public TaskStatusChanged(String taskId, TaskStatus status, String logFileName, Serializable execResult, String reason) {
        this.taskId = taskId;
        this.status = status;
        this.logFileName = logFileName;
        this.execResult = execResult;
        this.reason = reason;
    }

    //-------------------------------------------------------------------------------------------
    public static TaskStatusChanged finished(String taskId, String logFileName, String reason, Serializable execResult) {
        return new TaskStatusChanged(taskId, TaskStatus.FINISHED, logFileName, execResult, "task finished");
    }

    public static TaskStatusChanged fail(String taskId, String logFileName, String reason) {
        return new TaskStatusChanged(taskId, TaskStatus.FAIL, logFileName, null, reason);
    }

    public static TaskStatusChanged cancelled(String taskId, String logFileName, String reason) {
        return new TaskStatusChanged(taskId, TaskStatus.CANCELLED, logFileName, null, reason);
    }

    public static TaskStatusChanged running(String taskId, String logFileName) {
        return new TaskStatusChanged(taskId, TaskStatus.RUNNING, logFileName, null, "");
    }

    //-------------------------------------------------------------------------------------------
    @Override
    public String toString() {
        return "TaskStatusChanged{" +
                "taskId='" + taskId + '\'' +
                ", status=" + status +
                ", logFileName='" + logFileName + '\'' +
                ", execResult=" + execResult +
                ", reason='" + reason + '\'' +
                '}';
    }

    //setter && getter
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public Serializable getExecResult() {
        return execResult;
    }

    public void setExecResult(Serializable execResult) {
        this.execResult = execResult;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}