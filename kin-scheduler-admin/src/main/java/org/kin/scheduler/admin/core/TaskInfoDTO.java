package org.kin.scheduler.admin.core;

/**
 * @author huangjianqin
 * @date 2020-03-11
 */
public class TaskInfoDTO {
    private int jobId;
    private int taskId;
    private String desc;
    private int nowRetryTimes;
    private int retryTimesLimit;
    private int execTimeout;
    private String type;
    private String execStrategy;
    private String routeStrategy;
    private String param;

    public TaskInfoDTO(int jobId, int taskId, String desc, int nowRetryTimes, int retryTimesLimit, int execTimeout, String type, String execStrategy, String routeStrategy, String param) {
        this.jobId = jobId;
        this.taskId = taskId;
        this.desc = desc;
        this.nowRetryTimes = nowRetryTimes;
        this.retryTimesLimit = retryTimesLimit;
        this.execTimeout = execTimeout;
        this.type = type;
        this.execStrategy = execStrategy;
        this.routeStrategy = routeStrategy;
        this.param = param;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getNowRetryTimes() {
        return nowRetryTimes;
    }

    public void setNowRetryTimes(int nowRetryTimes) {
        this.nowRetryTimes = nowRetryTimes;
    }

    public int getRetryTimesLimit() {
        return retryTimesLimit;
    }

    public void setRetryTimesLimit(int retryTimesLimit) {
        this.retryTimesLimit = retryTimesLimit;
    }

    public int getExecTimeout() {
        return execTimeout;
    }

    public void setExecTimeout(int execTimeout) {
        this.execTimeout = execTimeout;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}
