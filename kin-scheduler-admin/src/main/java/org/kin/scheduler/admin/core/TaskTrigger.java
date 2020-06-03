package org.kin.scheduler.admin.core;

import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.admin.domain.Constants;
import org.kin.scheduler.admin.domain.TaskType;
import org.kin.scheduler.admin.entity.JobInfo;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.core.driver.route.RouteStrategyType;
import org.kin.scheduler.core.driver.scheduler.TaskExecCallback;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.task.TaskExecStrategy;
import org.kin.scheduler.core.task.domain.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
public class TaskTrigger {
    private static Logger log = LoggerFactory.getLogger(TaskTrigger.class);
    private static TaskTrigger INSTANCE;
    // email alarm template
    private static final String mailBodyTemplate = "<h5>" + "：</span>" +
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


    public void trigger(int taskId) {
        TaskInfo taskInfo = KinSchedulerContext.instance().getTaskInfoDao().load(taskId);
        if (Objects.isNull(taskInfo)) {
            log.warn("不存在task");
        }
        trigger(taskInfo);
    }

    public void trigger(TaskInfo taskInfo) {
        //调度job
        TaskExecFuture<Serializable> f = KinSchedulerContext.instance().getDriver().submitTask(taskInfo.convert());
        if (Objects.nonNull(f)) {
            f.addCallback(taskSubmitCallback);
        }
    }

    public void trigger(TaskInfoDTO taskInfoDTO) {
        //调度job
        TaskExecFuture<Serializable> f = KinSchedulerContext.instance().getDriver().submitTask(taskInfoDTO);
        if (Objects.nonNull(f)) {
            f.addCallback(taskSubmitCallback);
        }
    }

    private class AdminTaskExecCallback implements TaskExecCallback<Serializable> {
        private AdminTaskExecCallback() {
        }

        @Override
        public void execFinish(String taskId, TaskStatus taskStatus, Serializable result, String logPath, String outputPath, String reason) {
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
            TaskLog taskLog = KinSchedulerContext.instance().getTaskLogDao().load(Integer.parseInt(taskLogIdStr));
            if (Objects.isNull(taskLog)) {
                log.error("task({}) log not found", taskId);
            }
            if (taskLog.getHandleCode() > 0) {
                log.warn("task({}) log repeate", taskId);
            }
            taskLog.setHandleTime(new Date());
            taskLog.setHandleCode(taskStatus == TaskStatus.FINISHED ? Constants.SUCCESS_CODE : Constants.FAIL_CODE);

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

            KinSchedulerContext.instance().getTaskLogDao().updateHandleInfo(taskLog);

            if (isRaiseAlarm && StringUtils.isNotBlank(taskInfo.getAlarmEmail())) {
                JobInfo jobInfo = KinSchedulerContext.instance().getJobInfoDao().load(taskLog.getJobId());

                StringBuffer triggerMsg = new StringBuffer();
                triggerMsg.append("<br>调度时间: ").append(TimeUtils.formatDateTime(taskLog.getTriggerTime()));
                triggerMsg.append("<br>调度结果: ").append(taskLog.getTriggerCode() == Constants.SUCCESS_CODE ? "成功" : "失败");
                triggerMsg.append("<br>Executor地址: ").append(taskLog.getExecutorAddress());
                triggerMsg.append("<br>Task类型: ").append(TaskType.getByName(taskLog.getType()).getDesc());
                triggerMsg.append("<br>Task参数: ").append(taskLog.getParam());
                triggerMsg.append("<br>Task执行策略: ").append(TaskExecStrategy.getByName(taskLog.getExecStrategy()).getDesc());
                triggerMsg.append("<br>Executor路由策略: ").append(RouteStrategyType.getByName(taskLog.getRouteStrategy()).getDesc());
                triggerMsg.append("<br>Task执行超时限制: ").append(taskLog.getExecTimeout());
                triggerMsg.append("<br>Task当前尝试执行次数: ").append(taskLog.getRetryTimes());
                triggerMsg.append("<br>Task log地址: ").append(taskLog.getLogPath());
                triggerMsg.append("<br>Task output地址: ").append(taskLog.getOutputPath());

                StringBuffer handlerMsg = new StringBuffer();
                handlerMsg.append("<br>任务执行完成时间: ").append(TimeUtils.formatDateTime(taskLog.getHandleTime()));
                handlerMsg.append("<br>任务执行结果: ").append(taskLog.getHandleCode() == Constants.SUCCESS_CODE ? "成功" : "失败");
                handlerMsg.append("<br>任务执行结果描述: ").append(reason);

                StringBuffer contentSB = new StringBuffer();
                contentSB.append("Alarm Task LogId=" + taskLog.getId());
                contentSB.append("<br>TriggerMsg=" + triggerMsg.toString());
                contentSB.append("<br>HandleCode=" + handlerMsg.toString());

                String personal = "kin-scheduler-Admin";
                String title = "任务错误告警".concat("(Task-")
                        .concat(String.valueOf(taskLog.getId()))
                        .concat("-")
                        .concat(taskLog.getDesc())
                        .concat(")");
                String content = MessageFormat.format(mailBodyTemplate,
                        jobInfo != null ? jobInfo.getTitle() : "null",
                        taskLog.getTaskId(),
                        taskLog.getDesc(),
                        contentSB.toString());

                Set<String> emailSet = new HashSet<>(Arrays.asList(taskInfo.getAlarmEmail().split(",")));
                for (String email : emailSet) {
                    // make mail
                    try {
                        MimeMessage mimeMessage = KinSchedulerContext.instance().getMailSender().createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                        helper.setFrom(KinSchedulerContext.instance().getEmailUserName(), personal);
                        helper.setTo(email);
                        helper.setSubject(title);
                        helper.setText(content, true);

                        KinSchedulerContext.instance().getMailSender().send(mimeMessage);
                    } catch (Exception e) {
                        log.error("task fail alarm email send error, TaskLogId:{}", taskLog.getId(), e);
                    }

                }
            }
        }
    }

    private void tryTriggerAgain(TaskLog taskLog) {
        if (taskLog.getRetryTimes() < taskLog.getRetryTimesLimit()) {
            trigger(taskLog.convert());
            taskLog.setRetryTimes(taskLog.getRetryTimes() + 1);
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
