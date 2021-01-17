package org.kin.scheduler.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.kin.scheduler.admin.core.domain.TaskInfoDTO;

import java.util.Date;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@TableName(value = "task_log", autoResultMap = true)
public class TaskLog {
    /** 失败状态 */
    public static final int FAILURE = 0;
    /** 成功状态 */
    public static final int SUCCESS = 200;

    /** 唯一id */
    private Integer id;
    /** 所属taskId */
    private int taskId;
    /** 所属jobId */
    private int jobId;

    /** task描述 */
    private String desc;
    /** 执行task的executor地址 */
    private String executorAddress;
    /** 执行task的executor所属workerId */
    private String workerId;
    /** task类型 */
    private String type;
    /** 任务参数 */
    private String param;
    /** 任务执行策略 */
    private String execStrategy;
    /** 路由策略 */
    private String routeStrategy;
    /** 任务执行超时时间，单位秒 */
    private int execTimeout;
    /** 当前第n重试次数 */
    private int retryTimes;
    /** 重试次数上限 */
    private int retryTimesLimit;

    //------------------------------------------------trigger info------------------------------------------------------
    /** 调度时间 */
    private Date triggerTime;
    /** 调度结果 */
    private int triggerCode;

    //------------------------------------------------handle info-------------------------------------------------------
    /** task执行结束时间 */
    private Date handleTime;
    /** task执行结果 */
    private int handleCode;
    /** task执行日志 */
    private String logPath;
    /** task输出文件 */
    private String outputPath;

    /**
     * 转换成TaskInfoDTO, 用于重试, 所以当前重试+1
     */
    public TaskInfoDTO retry() {
        return new TaskInfoDTO(jobId, taskId, desc, retryTimes + 1, retryTimesLimit, execTimeout, type, execStrategy, routeStrategy, param);
    }

    //setter && getter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
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

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
}
