package org.kin.scheduler.admin.core;

import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.admin.core.domain.TaskInfoDTO;
import org.kin.scheduler.admin.domain.Constants;
import org.kin.scheduler.admin.domain.TaskType;
import org.kin.scheduler.admin.entity.JobInfo;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.admin.utils.MailUtils;
import org.kin.scheduler.core.driver.route.RouteStrategyType;
import org.kin.scheduler.core.driver.scheduler.TaskExecCallback;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.task.TaskExecStrategy;
import org.kin.scheduler.core.task.domain.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;

/**
 * 提交task工具类
 * 单例
 *
 * @author huangjianqin
 * @date 2020-03-08
 */
public class TaskTrigger {
    private static Logger log = LoggerFactory.getLogger(TaskTrigger.class);
    private static TaskTrigger INSTANCE;
    /** 警告邮件模板 */
    private static final String MAIL_BODY_TEMPLATE = "<h5>" + "：</span>" +
            "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
            "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
            "   </thead>\n" +
            "   <tbody>\n" +
            "      <tr>\n" +
            "         <td>{0}</td>\n" +
            "         <td>{1}</td>\n" +
            "         <td>{2}</td>\n" +
            "         <td>{3}</td>\n" +
            "      </tr>\n" +
            "   </tbody>\n" +
            "</table>";
    /** task执行回调callback, 单例 */
    private final AdminTaskExecCallback taskSubmitCallback = new AdminTaskExecCallback();

    public static TaskTrigger instance() {
        if (Objects.isNull(INSTANCE)) {
            synchronized (TaskTrigger.class) {
                if (Objects.isNull(INSTANCE)) {
                    INSTANCE = new TaskTrigger();
                }
            }
        }

        return INSTANCE;
    }

    /**
     * 提交task
     *
     * @param taskId task id
     */
    public void trigger(int taskId) {
        TaskInfo taskInfo = KinSchedulerContext.instance().getTaskInfoDao().load(taskId);
        if (Objects.isNull(taskInfo)) {
            log.warn("不存在task");
        }
        trigger(taskInfo);
    }

    /**
     * 提交task
     *
     * @param taskInfo task信息
     */
    public void trigger(TaskInfo taskInfo) {
        trigger(taskInfo.convert());
    }

    /**
     * 提交task
     *
     * @param taskInfoDTO task信息
     */
    private void trigger(TaskInfoDTO taskInfoDTO) {
        //提交task
        TaskExecFuture<Serializable> f = KinSchedulerContext.instance().getDriver().submitTask(taskInfoDTO);
        if (Objects.nonNull(f)) {
            f.addCallback(taskSubmitCallback);
        }
    }

    /**
     * task执行完成回调, 仅仅处理task执行过的情况, 提交失败的情况需要额外处理
     */
    private class AdminTaskExecCallback implements TaskExecCallback<Serializable> {
        private AdminTaskExecCallback() {
        }

        @Override
        public void execFinish(String taskId, TaskStatus taskStatus, Serializable result, String logPath, String outputPath, String reason) {
            //解析出tasklog id
            int index = logPath.lastIndexOf("/");
            if (index < 0) {
                log.error("task({}) log not found >>>>> {}, {}, {}, {}, {}", taskId, taskStatus, result, logPath, outputPath, reason);
                return;
            }
            String taskLogIdStr = logPath.substring(index);
            if (StringUtils.isBlank(taskLogIdStr)) {
                log.error("task({}) log not found >>>>> {}, {}, {}, {}, {}", taskId, taskStatus, result, logPath, outputPath, reason);
                return;
            }

            //获取tasklog
            TaskLog taskLog = KinSchedulerContext.instance().getTaskLogDao().load(Integer.parseInt(taskLogIdStr));
            if (Objects.isNull(taskLog)) {
                log.error("task({}) log not found", taskId);
            }
            if (taskLog.getHandleCode() > 0) {
                log.warn("task({}) log repeate", taskId);
            }
            taskLog.setHandleTime(new Date());
            taskLog.setHandleCode(taskStatus == TaskStatus.FINISHED ? Constants.SUCCESS_CODE : Constants.FAIL_CODE);

            KinSchedulerContext.instance().getTaskLogDao().updateHandleInfo(taskLog);

            boolean isRaiseAlarm = false;
            TaskInfo taskInfo = KinSchedulerContext.instance().getTaskInfoDao().load(Integer.parseInt(taskId));
            if (Objects.nonNull(taskInfo)) {
                if (taskStatus == TaskStatus.FINISHED) {
                    if (StringUtils.isNotBlank(taskInfo.getChildTaskIds())) {
                        String[] childTaskIdStrs = taskInfo.getChildTaskIds().split(",");
                        for (String childTaskIdStr : childTaskIdStrs) {
                            int childTaskId = Integer.parseInt(childTaskIdStr);
                            if (childTaskId > 0) {
                                TaskTrigger.this.trigger(childTaskId);
                            }
                        }
                    }
                } else {
                    //重试
                    tryTriggerAgain(taskLog);
                    isRaiseAlarm = true;
                }
            }

            if (isRaiseAlarm) {
                sendAlarmEmail(taskInfo, taskLog, reason);
            }
        }
    }

    /**
     * 发送警告邮件
     *
     * @param taskInfo task信息
     * @param taskLog  task执行log信息
     * @param reason   task执行具体信息(失败或成功)
     */
    private void sendAlarmEmail(TaskInfo taskInfo, TaskLog taskLog, String reason) {
        if (StringUtils.isBlank(taskInfo.getAlarmEmail())) {
            return;
        }

        JobInfo jobInfo = KinSchedulerContext.instance().getJobInfoDao().load(taskLog.getJobId());

        StringBuffer triggerMsg = new StringBuffer();
        triggerMsg.append("<br>调度时间: ").append(TimeUtils.formatDateTime(taskLog.getTriggerTime()));
        triggerMsg.append("<br>调度结果: ").append(taskLog.getTriggerCode() == Constants.SUCCESS_CODE ? "成功" : "失败");
        triggerMsg.append("<br>Executor地址: ").append(taskLog.getExecutorAddress());
        triggerMsg.append("<br>Task类型: ").append(TaskType.getDescByName(taskLog.getType()));
        triggerMsg.append("<br>Task参数: ").append(taskLog.getParam());
        triggerMsg.append("<br>Task执行策略: ").append(TaskExecStrategy.getDescByName(taskLog.getExecStrategy()));
        triggerMsg.append("<br>Executor路由策略: ").append(RouteStrategyType.getDescByName(taskLog.getRouteStrategy()));
        triggerMsg.append("<br>Task执行超时限制: ").append(taskLog.getExecTimeout());
        triggerMsg.append("<br>Task当前尝试执行次数: ").append(taskLog.getRetryTimes());
        triggerMsg.append("<br>Task log地址: ").append(taskLog.getLogPath());
        triggerMsg.append("<br>Task output地址: ").append(taskLog.getOutputPath());

        StringBuffer handlerMsg = new StringBuffer();
        handlerMsg.append("<br>任务执行完成时间: ").append(TimeUtils.formatDateTime(taskLog.getHandleTime()));
        handlerMsg.append("<br>任务执行结果: ").append(taskLog.getHandleCode() == Constants.SUCCESS_CODE ? "成功" : "失败");
        handlerMsg.append("<br>任务执行结果描述: ").append(reason);

        StringBuffer contentSB = new StringBuffer();
        contentSB.append("Alarm Task LogId=").append(taskLog.getId());
        contentSB.append("<br>TriggerMsg=").append(triggerMsg.toString());
        contentSB.append("<br>HandleCode=").append(handlerMsg.toString());

        String personal = "kin-scheduler-Admin";
        String title = "任务错误告警"
                .concat("(Job-").concat(String.valueOf(jobInfo.getId()))
                .concat("Task-").concat(String.valueOf(taskLog.getId()))
                .concat("-").concat(taskLog.getDesc()).concat(")");
        String content = MessageFormat.format(MAIL_BODY_TEMPLATE,
                jobInfo.getTitle(),
                taskLog.getTaskId(),
                taskLog.getDesc(),
                contentSB.toString());

        Set<String> emailSet = new HashSet<>(Arrays.asList(taskInfo.getAlarmEmail().split(",")));
        for (String email : emailSet) {
            try {
                //发送警告邮件
                MailUtils.sendHtmlMail(KinSchedulerContext.instance().getEmailUserName(), personal, email, title, content);
            } catch (Exception e) {
                log.error("task fail alarm email send error, TaskLogId:{}", taskLog.getId(), e);
            }

        }
    }

    /**
     * task提交失败重试
     */
    private void tryTriggerAgain(TaskLog taskLog) {
        if (taskLog.getRetryTimes() < taskLog.getRetryTimesLimit()) {
            //还有重试次数, 尝试重新提交
            trigger(taskLog.retry());
        } else {
            //没有重试次数
            TaskInfo taskInfo = KinSchedulerContext.instance().getTaskInfoDao().load(taskLog.getTaskId());
            if (Objects.isNull(taskInfo)) {
                return;
            }
            //发送警告邮件
            sendAlarmEmail(taskInfo, taskLog, String.format("task提交重试次数已超过%s次", taskLog.getRetryTimesLimit()));
        }
    }

    /**
     * 提交task失败
     */
    public void submitTaskFail(TaskLog taskLog) {
        taskLog.setHandleCode(Constants.FAIL_CODE);
        tryTriggerAgain(taskLog);

        KinSchedulerContext.instance().getTaskLogDao().updateHandleInfo(taskLog);
    }
}
