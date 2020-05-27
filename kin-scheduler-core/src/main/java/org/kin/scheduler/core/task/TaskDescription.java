package org.kin.scheduler.core.task;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-05
 * <p>
 * task信息的抽象
 */
public class TaskDescription<PARAM extends Serializable> implements Serializable {
    private String jobId;
    private String taskId;
    /** 自定义Task参数类型 */
    private PARAM param;
    /** Task执行策略 */
    private TaskExecStrategy execStrategy;
    /** Task执行超时, <0 表示不等待, 0一直等待, >0 表示超时等待 */
    private int timeout = -1;
    //log文件名，发送者定义, 可以是重试次数1 2 3, 可以是自定义唯一Name
    private String logFileName;

    public TaskDescription() {
    }

    public TaskDescription(String jobId, String taskId) {
        this.jobId = jobId;
        this.taskId = taskId;
    }

    //--------------------------------------------------------------------------------------------

    /**
     * 创建一个尚未分配id的Task
     */
    public static <PARAM extends Serializable> TaskDescription<PARAM> createTmpTask(PARAM param, TaskExecStrategy execStrategy, int timeout) {
        TaskDescription<PARAM> newTaskDescription = new TaskDescription<>();
        newTaskDescription.setParam(param);
        newTaskDescription.setExecStrategy(execStrategy);
        newTaskDescription.setTimeout(timeout);
        return newTaskDescription;
    }


    //--------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskDescription<?> taskDescription = (TaskDescription<?>) o;
        return Objects.equals(taskId, taskDescription.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return "Task{" +
                "jobId='" + jobId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", param=" + param +
                ", execStrategy=" + execStrategy +
                ", timeout=" + timeout +
                '}';
    }

    //setter && getter

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public PARAM getParam() {
        return param;
    }

    public void setParam(PARAM param) {
        this.param = param;
    }

    public TaskExecStrategy getExecStrategy() {
        return execStrategy;
    }

    public void setExecStrategy(TaskExecStrategy execStrategy) {
        this.execStrategy = execStrategy;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }
}
