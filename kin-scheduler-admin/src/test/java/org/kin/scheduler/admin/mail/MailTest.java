package org.kin.scheduler.admin.mail;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

/**
 * @author huangjianqin
 * @date 2020-06-23
 */
@SpringBootApplication
public class MailTest implements InitializingBean {
    private static MailTest INSTANCE;
    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String emailUserName;

    public void run() {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(emailUserName, "kin-scheduler-Admin");
            //TODO 邮箱地址
//            helper.setTo("");
            helper.setSubject("邮件测试");
            helper.setText("<br>测试1: 111".concat("<br>测试2: 2222"), true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            System.err.println(e);
        } catch (UnsupportedEncodingException e) {
            System.err.println(e);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MailTest.class);
        MailTest.INSTANCE.run();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }
}
