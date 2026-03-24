package com.removel.accp.util;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String FROM_EMAIL;

    /**
     * 发送纯文本邮件
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param message 邮件内容
     */
    // 目前就先写这一个吧，后面有需求再写别的
    public void sendEmail(String toEmail, String subject, String message) {
        SimpleMailMessage email = new SimpleMailMessage();
        //发件人
        email.setFrom(FROM_EMAIL);
        //收件人
        email.setTo(toEmail);
        //邮件主题
        email.setSubject(subject);
        //邮件内容
        email.setText(message);
        //发送邮件
        mailSender.send(email);
    }

}
