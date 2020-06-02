package org.kin.scheduler.core.executor.transport;

import org.kin.framework.utils.StringUtils;
import org.kin.scheduler.core.transport.RPCResult;

/**
 * @author huangjianqin
 * @date 2020-03-10
 * <p>
 * task 提交结果
 */
public class TaskSubmitResult extends RPCResult {
    private String taskId;
    private String logPath;
    private String outputPath;

    public TaskSubmitResult() {
    }

    public TaskSubmitResult(boolean success, String desc) {
        super(success, desc);
    }

    public TaskSubmitResult(boolean success, String desc, String taskId, String logPath, String outputPath) {
        super(success, desc);
        this.taskId = taskId;
        this.logPath = StringUtils.isNotBlank(logPath) ? logPath : "";
        this.outputPath = StringUtils.isNotBlank(outputPath) ? outputPath : "";
    }

    public static TaskSubmitResult success(String taskId, String logPath, String outputPath) {
        return new TaskSubmitResult(true, "", taskId, logPath, outputPath);
    }

    public static TaskSubmitResult failure(String taskId, String desc) {
        return new TaskSubmitResult(false, desc, taskId, "", "");
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

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }


}
