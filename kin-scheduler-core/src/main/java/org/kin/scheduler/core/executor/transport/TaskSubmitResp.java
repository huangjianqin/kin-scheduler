package org.kin.scheduler.core.executor.transport;

import org.kin.framework.utils.StringUtils;
import org.kin.scheduler.core.transport.RPCResp;

/**
 * task 提交返回消息
 *
 * @author huangjianqin
 * @date 2020-03-10
 */
public class TaskSubmitResp extends RPCResp {
    private static final long serialVersionUID = -7447918191273892930L;
    /** task id */
    private String taskId;
    /** task log路径 */
    private String logPath;
    /** task output路径 */
    private String outputPath;

    public static TaskSubmitResp of(boolean success, String desc) {
        TaskSubmitResp message = new TaskSubmitResp();
        message.success = success;
        message.desc = desc;
        return message;
    }

    public static TaskSubmitResp of(boolean success, String desc, String taskId, String logPath, String outputPath) {
        TaskSubmitResp message = TaskSubmitResp.of(success, desc);
        message.taskId = taskId;
        message.logPath = StringUtils.isNotBlank(logPath) ? logPath : "";
        message.outputPath = StringUtils.isNotBlank(outputPath) ? outputPath : "";
        return message;
    }

    public static TaskSubmitResp success(String taskId, String logPath, String outputPath) {
        return TaskSubmitResp.of(true, "", taskId, logPath, outputPath);
    }

    public static TaskSubmitResp failure(String taskId, String desc) {
        return TaskSubmitResp.of(false, desc, taskId, "", "");
    }

    //---------------------------------------------------------------------------------------------------
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
