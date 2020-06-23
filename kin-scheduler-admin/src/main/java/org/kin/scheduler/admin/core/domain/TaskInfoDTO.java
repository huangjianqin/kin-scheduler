package org.kin.scheduler.admin.core.domain;

/**
 * admin模块中task相关参数封装, 然后会转换成通过task描述并提交task
 *
 * @author huangjianqin
 * @date 2020-03-11
 */
public class TaskInfoDTO {
    /** 所属jobId */
    private int jobId;
    /** 所属taskId */
    private int taskId;
    /** task描述 */
    private String desc;
    /** 当前第n重试次数 */
    private int nowRetryTimes;
    /** 重试次数上限 */
    private int retryTimesLimit;
    /** 任务执行超时时间，单位秒 */
    private int execTimeout;
    /** task类型 */
    private String type;
    /** 任务执行策略 */
    private String execStrategy;
    /** 路由策略 */
    private String routeStrategy;
    /** 任务参数 */
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
