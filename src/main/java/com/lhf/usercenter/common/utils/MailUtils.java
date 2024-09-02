package com.lhf.usercenter.common.utils;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.lhf.usercenter.contant.UserConstant;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public final class MailUtils {

    // 发件人邮箱和授权码配置
    private static final String QQ_USER = "2385107101@qq.com";
    private static final String QQ_PASSWORD = "oupvfynkmjziebjj";

    private static final String GMAIL_USER = "lhf97777@gmail.com";
    private static final String GMAIL_PASSWORD = "bffn xfql harj zezr";

    private static final String USER_163 = "15671389027@163.com";
    private static final String PASSWORD_163 = "KMCWIZJMOOOWGIQF";

    /**
     * 发送邮件验证码
     *
     * @param to         收件人邮箱
     * @param verifyCode 邮件验证码
     * @return 是否发送成功
     */
    public static boolean sendMail(String to, String verifyCode, int method) {
        String[] hostAndPort = getSmtpHostAndPort(to);
        String[] senderDetails = getSenderEmailAndPassword(to);

        if (hostAndPort == null || senderDetails == null) {
            System.err.println("Unsupported email domain or no sender details provided");
            return false;
        }

        String host = hostAndPort[0];
        String port = hostAndPort[1];
        String user = senderDetails[0];
        String password = senderDetails[1];

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        // 设置加密协议
        if ("587".equals(port)) {
            props.put("mail.smtp.starttls.enable", "true"); // 使用STARTTLS加密
        } else if ("465".equals(port)) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // 使用SSL加密
        }

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        };

        try {
            Session mailSession = Session.getInstance(props, authenticator);
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(user));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            String time = LocalDateTimeUtil.format(LocalDateTimeUtil.now(), "yyyy-MM-dd HH:mm:ss");
            String text = "";
            if (method == UserConstant.USER_REGISTER) {
                message.setSubject("MeetFriends 注册验证码");
                text = "<html>" +
                        "<body>" +
                        "<p>尊敬的用户：</p>" +
                        "<p>您好！</p>" +
                        "<p>欢迎您注册 <strong>MeetFriends</strong>。</p>" +
                        "<p>您本次注册所需的验证码：<strong>" + verifyCode + "</strong></p>" +
                        "<p>请您在 <u>5 分钟内完成验证</u>。如果验证码过期或遇到任何问题，请重新点击获取验证码。</p>" +
                        "<p><strong>注意！</strong>此验证码涉及到您的账户安全，请不要随意告知他人。</p>" +
                        "<p>感谢您的支持与配合，祝您注册顺利！</p>" +
                        "<p><small>时间：" + time + "</small></p>" +
                        "<p>如有问题，请联系我们 <a href=\"http://www.meetfei.cn\" target=\"_blank\">帮助中心</a>。</p>" +
                        "</body>" +
                        "</html>";
            } else {
                message.setSubject("MeetFriends 重置密码");
                text = "<html>" +
                        "<body>" +
                        "<p>尊敬的用户：</p>" +
                        "<p>您的新密码为：<strong>" + verifyCode + "</strong></p>" +
                        "<p><small>时间：" + time + "</small></p>" +
                        "</body>" +
                        "</html>";
            }
            message.setContent(text, "text/html;charset=UTF-8");
            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 根据邮箱地址的域名返回SMTP邮件服务器地址和端口
     *
     * @param email 收件人邮箱地址
     * @return SMTP邮件服务器地址和端口
     */
    private static String[] getSmtpHostAndPort(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        switch (domain) {
            case "qq.com":
                return new String[]{"smtp.qq.com", "587"};
            case "163.com":
                return new String[]{"smtp.163.com", "465 "};
            case "gmail.com":
                return new String[]{"smtp.gmail.com", "587"};
            case "yahoo.com":
                return new String[]{"smtp.mail.yahoo.com", "587"};
            default:
                return null; // 其他未支持的邮箱域名
        }
    }

    /**
     * 根据邮箱地址的域名返回对应的发件人邮箱和授权码
     *
     * @param email 收件人邮箱地址
     * @return 发件人邮箱和对应的授权码
     */
    private static String[] getSenderEmailAndPassword(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        switch (domain) {
            case "qq.com":
                return new String[]{QQ_USER, QQ_PASSWORD};
            case "163.com":
                return new String[]{USER_163, PASSWORD_163};
            case "gmail.com":
                return new String[]{GMAIL_USER, GMAIL_PASSWORD};
            default:
                return null; // 其他未支持的邮箱域名
        }
    }
}
