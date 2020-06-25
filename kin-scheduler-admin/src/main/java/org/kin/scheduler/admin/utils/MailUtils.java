package org.kin.scheduler.admin.utils;

import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.scheduler.admin.core.KinSchedulerContext;
import org.kin.scheduler.admin.domain.TaskType;
import org.kin.scheduler.admin.entity.JobInfo;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.kin.scheduler.admin.entity.TaskLog;
import org.kin.scheduler.core.driver.route.RouteStrategyType;
import org.kin.scheduler.core.task.TaskExecStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 邮件工具类
 *
 * @author huangjianqin
 * @date 2020-06-23
 */
public class MailUtils {
    private static final Logger log = LoggerFactory.getLogger(MailUtils.class);
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

    /**
     * 发送警告邮件
     *
     * @param taskInfo task信息
     * @param taskLog  task执行log信息
     * @param reason   task执行具体信息(失败或成功)
     */
    public static void sendAlarmEmail(TaskInfo taskInfo, TaskLog taskLog, String reason) {
        if (StringUtils.isBlank(taskInfo.getAlarmEmail())) {
            return;
        }

        JobInfo jobInfo = KinSchedulerContext.instance().getJobInfoDao().load(taskLog.getJobId());

        StringBuffer triggerMsg = new StringBuffer();
        triggerMsg.append("<br>调度时间: ").append(TimeUtils.formatDateTime(taskLog.getTriggerTime()));
        triggerMsg.append("<br>调度结果: ").append(taskLog.getTriggerCode() == TaskLog.SUCCESS ? "成功" : "失败");
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
        handlerMsg.append("<br>任务执行结果: ").append(taskLog.getHandleCode() == TaskLog.SUCCESS ? "成功" : "失败");
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

    public static void sendMail(String from, String to, String title, String content) throws Exception {
        sendMail(from, from, to, title, content, false);
    }

    public static void sendHtmlMail(String from, String to, String title, String content) throws Exception {
        sendMail(from, from, to, title, content, true);
    }

    public static void sendMail(String from, String personal, String to, String title, String content) throws Exception {
        sendMail(from, personal, to, title, content, false);
    }

    public static void sendHtmlMail(String from, String personal, String to, String title, String content) throws Exception {
        sendMail(from, personal, to, title, content, true);
    }

    /**
     * 发送邮件, 发送方通过配置完成
     *
     * @param from     发送方邮箱地址
     * @param personal 邮件昵称, 接收方看到发送反的昵称
     * @param to       接受方邮箱地址
     * @param title    标题
     * @param content  内容
     * @param html     标识内容是否是html格式
     * @throws Exception
     */
    public static void sendMail(String from, String personal, String to, String title, String content, boolean html) throws Exception {
        JavaMailSender mailSender = KinSchedulerContext.instance().getMailSender();
        // make mail
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom(from, personal);
        helper.setTo(to);
        helper.setSubject(title);
        helper.setText(content, html);

        mailSender.send(mimeMessage);
    }
}
