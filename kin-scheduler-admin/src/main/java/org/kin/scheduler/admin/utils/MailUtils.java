package org.kin.scheduler.admin.utils;

import org.kin.scheduler.admin.core.KinSchedulerContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;

/**
 * 邮件工具类
 *
 * @author huangjianqin
 * @date 2020-06-23
 */
public class MailUtils {
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
