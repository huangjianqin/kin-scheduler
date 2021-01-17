package org.kin.scheduler.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.kin.scheduler.admin.core.domain.TaskInfoDTO;

import java.util.Date;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
@TableName(value = "task_info", autoResultMap = true)
public class TaskInfo {
    /** 初始触发状态 */
    public static final int INIT = 0;
    /** 触发中 */
    public static final int START = 1;
    /** 触发结束 */
    public static final int END = 2;

    /** 唯一id */
    private Integer id;
    /** 所属jobId */
    private int jobId;
    /** 时间类型 */
    private String timeType;
    /** 时间表达式 */
    private String timeStr;
    /** task描述 */
    private String desc;
    /** 添加日期 */
    private Date addTime;
    /** 更新日期 */
    private Date updateTime;
    /** 负责人 */
    private int userId;
    /** 路由策略 */
    private String routeStrategy;
    /** task类型 */
    private String type;
    /** 任务参数 */
    private String param;
    /** 任务执行策略 */
    private String execStrategy;
    /** 任务执行超时时间，单位秒 */
    private int execTimeout;
    /** 失败重试次数 */
    private int retryTimes;
    /** 告警邮箱 */
    private String alarmEmail;

    /** 子任务ID，多个逗号分隔 */
    private String childTaskIds = "";

    /** 调度状态 */
    private int triggerStatus;
    /** 上次调度时间 */
    private long triggerLastTime;
    /** 下次调度时间 */
    private long triggerNextTime;

    /**
     * 重置触发状态
     */
    public void stop() {
        setTriggerStatus(INIT);
        setTriggerLastTime(0);
        setTriggerNextTime(0);
    }

    /**
     * 转换成初始的TaskInfoDTO
     */
    public TaskInfoDTO convert() {
        return new TaskInfoDTO(jobId, id, desc, 0, retryTimes, execTimeout, type,
                execStrategy, routeStrategy, param);
    }

    /**
     * 触发结束
     */
    public void end() {
        setTriggerStatus(END);
    }

    //setter && getter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getTimeType() {
        return timeType;
    }

    public void setTimeType(String timeType) {
        this.timeType = timeType;
    }

    public String getTimeStr() {
        return timeStr;
    }

    public void setTimeStr(String timeStr) {
        this.timeStr = timeStr;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRouteStrategy() {
        return routeStrategy;
    }

    public void setRouteStrategy(String routeStrategy) {
        this.routeStrategy = routeStrategy;
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

    public String getAlarmEmail() {
        return alarmEmail;
    }

    public void setAlarmEmail(String alarmEmail) {
        this.alarmEmail = alarmEmail;
    }

    public String getChildTaskIds() {
        return childTaskIds;
    }

    public void setChildTaskIds(String childTaskIds) {
        this.childTaskIds = childTaskIds;
    }

    public int getTriggerStatus() {
        return triggerStatus;
    }

    public void setTriggerStatus(int triggerStatus) {
        this.triggerStatus = triggerStatus;
    }

    public long getTriggerLastTime() {
        return triggerLastTime;
    }

    public void setTriggerLastTime(long triggerLastTime) {
        this.triggerLastTime = triggerLastTime;
    }

    public long getTriggerNextTime() {
        return triggerNextTime;
    }

    public void setTriggerNextTime(long triggerNextTime) {
        this.triggerNextTime = triggerNextTime;
    }
}
