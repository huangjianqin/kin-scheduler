package org.kin.scheduler.admin.entity;

import org.kin.scheduler.admin.core.TaskInfoDTO;

import java.util.Date;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public class TaskLog {
    private int id;
    private int taskId;
    private int jobId;

    private String desc;
    private String executorAddress;
    private String type;
    private String param;
    private String execStrategy;
    private String routeStrategy;
    private int execTimeout;
    //当前第n重试次数
    private int retryTimes;
    //重试次数上限
    private int retryTimesLimit;

    // trigger info
    private Date triggerTime;
    private int triggerCode;
    // handle info
    private Date handleTime;
    private int handleCode;
    //task执行日志
    private String logPath;

    public TaskInfoDTO convert() {
        return new TaskInfoDTO(jobId, taskId, desc, retryTimes + 1, retryTimesLimit, execTimeout, type, execStrategy, routeStrategy, param);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getExecutorAddress() {
        return executorAddress;
    }

    public void setExecutorAddress(String executorAddress) {
        this.executorAddress = executorAddress;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getExecStrategy() {
        return execStrategy;
    }

    public void setExecStrategy(String execStrategy) {
        this.execStrategy = execStrategy;
    }

    public String getRouteStrategy() {
        return routeStrategy;
    }

    public void setRouteStrategy(String routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public int getExecTimeout() {
        return execTimeout;
    }

    public void setExecTimeout(int execTimeout) {
        this.execTimeout = execTimeout;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public int getRetryTimesLimit() {
        return retryTimesLimit;
    }

    public void setRetryTimesLimit(int retryTimesLimit) {
        this.retryTimesLimit = retryTimesLimit;
    }

    public Date getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(Date triggerTime) {
        this.triggerTime = triggerTime;
    }

    public int getTriggerCode() {
        return triggerCode;
    }

    public void setTriggerCode(int triggerCode) {
        this.triggerCode = triggerCode;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
}
