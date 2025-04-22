package com.example.demo.utils;

import com.example.demo.config.MailConfig;
import com.sun.mail.util.MailSSLSocketFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class MailUtils {
    private final MailConfig mailConfig;

    public void sendApplicationEmail(String userEmail, String applyId, String status, String reason) {
//        try {
//            Properties props = new Properties();
//            props.put("mail.smtp.host", mailConfig.getHost());
//            props.put("mail.smtp.port", mailConfig.getPort());
//            props.put("mail.smtp.auth", mailConfig.getProperties().getSmtp().isAuth());
//            props.put("mail.smtp.starttls.enable", mailConfig.getProperties().getSmtp().getStarttls().isEnable());
//            props.put("mail.smtp.ssl.enable", mailConfig.getProperties().getSmtp().isSsl());
//            props.put("mail.smtp.timeout", mailConfig.getProperties().getSmtp().getTimeout());
//
//            // QQ邮箱需要SSL
//            MailSSLSocketFactory sf = new MailSSLSocketFactory();
//            sf.setTrustAllHosts(true);
//            props.put("mail.smtp.ssl.socketFactory", sf);
//            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
//
//            Session session = Session.getInstance(props, new Authenticator() {
//                @Override
//                protected PasswordAuthentication getPasswordAuthentication() {
//                    return new PasswordAuthentication(mailConfig.getUsername(), mailConfig.getPassword());
//                }
//            });
//
//            session.setDebug(true);
//
//            MimeMessage message = new MimeMessage(session);
//            message.setFrom(new InternetAddress(mailConfig.getFrom()));
//            message.addRecipient(Message.RecipientType.TO, new InternetAddress(userEmail));
//
//            String subject;
//            String content;
//            switch (status.toLowerCase()) {
//                case "submitted":
//                    subject = "下载申请已提交";
//                    content = String.format("您的申请（ID：%s）已提交，等待审核。", applyId);
//                    break;
//                case "approved":
//                    subject = "下载申请已通过";
//                    content = String.format("您的下载申请（ID：%s）已通过审核。", applyId);
//                    break;
//                case "rejected":
//                    subject = "下载申请被拒绝";
//                    content = String.format("您的下载申请（ID：%s）被拒绝。原因：%s", applyId, reason);
//                    break;
//                default:
//                    throw new IllegalArgumentException("无效状态: " + status);
//            }
//
//            message.setSubject(subject);
//            message.setText(content);
//
//            Transport.send(message);
//        } catch (Exception e) {
//            throw new RuntimeException("发送邮件失败: " + e.getMessage(), e);
//        }
    }

    public void sendDownloadLinkEmail(String userEmail, String applyId, String downloadLink) {
//        try {
//            Properties props = new Properties();
//            props.put("mail.smtp.host", mailConfig.getHost());
//            props.put("mail.smtp.port", mailConfig.getPort());
//            props.put("mail.smtp.auth", mailConfig.getProperties().getSmtp().isAuth());
//            props.put("mail.smtp.starttls.enable", mailConfig.getProperties().getSmtp().getStarttls().isEnable());
//            props.put("mail.smtp.ssl.enable", mailConfig.getProperties().getSmtp().isSsl());
//            props.put("mail.smtp.timeout", mailConfig.getProperties().getSmtp().getTimeout());
//
//            // QQ邮箱需要SSL
//            MailSSLSocketFactory sf = new MailSSLSocketFactory();
//            sf.setTrustAllHosts(true);
//            props.put("mail.smtp.ssl.socketFactory", sf);
//            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
//
//            Session session = Session.getInstance(props, new Authenticator() {
//                @Override
//                protected PasswordAuthentication getPasswordAuthentication() {
//                    return new PasswordAuthentication(mailConfig.getUsername(), mailConfig.getPassword());
//                }
//            });
//
//            MimeMessage message = new MimeMessage(session);
//            message.setFrom(new InternetAddress(mailConfig.getFrom()));
//            message.addRecipient(Message.RecipientType.TO, new InternetAddress(userEmail));
//
//            // 设置邮件主题和内容
//            String subject = "您的下载数据已准备好";
//            String content = String.format(
//                    "<html>" +
//                            "<body>" +
//                            "<h3>您的数据下载申请（ID：%s）已处理完成</h3>" +
//                            "<p>下载链接：<a href='%s'>点击下载</a></p>" +
//                            "<p>或复制以下链接到浏览器：%s</p>" +
//                            "<p>链接有效期：7天</p>" +
//                            "</body>" +
//                            "</html>",
//                    applyId, downloadLink, downloadLink);
//
//            message.setSubject(subject);
//            message.setContent(content, "text/html;charset=UTF-8"); // 设置为HTML格式
//
//            Transport.send(message);
//        } catch (Exception e) {
//            throw new RuntimeException("发送下载链接邮件失败: " + e.getMessage(), e);
//        }
    }
}