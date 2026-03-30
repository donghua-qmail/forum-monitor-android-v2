package com.hexin.forummonitor;

import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 通知管理类
 * 功能：发送Server酱微信通知和QQ邮件通知
 */
public class Notifier {

    private static final String TAG = "Notifier";

    private static final String SERVER_CHAN_API = "https://sctapi.ftqq.com/";
    private static final String QQ_SMTP_HOST = "smtp.qq.com";
    private static final int QQ_SMTP_PORT = 587;

    private OkHttpClient httpClient;

    public Notifier() {
        // 配置HTTP客户端
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    }

    /**
     * 发送Server酱微信通知
     *
     * @param key   Server酱SendKey
     * @param title 通知标题
     * @param desc  通知内容
     * @return 是否发送成功
     */
    public boolean sendServerChan(String key, String title, String desc) {
        if (key == null || key.isEmpty()) {
            Log.w(TAG, "Server酱Key为空，跳过通知");
            return false;
        }

        new Thread(() -> {
            try {
                RequestBody formBody = new FormBody.Builder()
                    .add("title", title)
                    .add("desp", desc)
                    .build();

                Request request = new Request.Builder()
                    .url(SERVER_CHAN_API + key + ".send")
                    .post(formBody)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String body = response.body() != null ? response.body().string() : "";
                        Log.i(TAG, "Server酱通知成功: " + body);
                    } else {
                        Log.e(TAG, "Server酱通知失败: " + response.code());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "发送Server酱通知异常", e);
            }
        }).start();

        return true;
    }

    /**
     * 发送QQ邮件通知
     *
     * @param fromAccount 发件邮箱（QQ邮箱）
     * @param authCode    授权码
     * @param toEmail     收件邮箱
     * @param subject     邮件主题
     * @param content     邮件内容
     * @return 是否发送成功
     */
    public boolean sendEmail(String fromAccount, String authCode,
                            String toEmail, String subject, String content) {
        if (fromAccount == null || fromAccount.isEmpty() ||
            authCode == null || authCode.isEmpty() ||
            toEmail == null || toEmail.isEmpty()) {
            Log.w(TAG, "邮箱配置不完整，跳过邮件通知");
            return false;
        }

        new Thread(() -> {
            try {
                // 配置邮件会话
                Properties props = new Properties();
                props.put("mail.smtp.host", QQ_SMTP_HOST);
                props.put("mail.smtp.port", QQ_SMTP_PORT);
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.ssl.trust", QQ_SMTP_HOST);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(fromAccount, authCode);
                    }
                });

                // 创建邮件消息
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromAccount));
                message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setText(content);

                // 发送邮件
                Transport.send(message);
                Log.i(TAG, "邮件发送成功");

            } catch (Exception e) {
                Log.e(TAG, "发送邮件失败", e);
            }
        }).start();

        return true;
    }

    /**
     * 发送监控发现通知
     *
     * @param key          Server酱Key
     * @param emailAccount 发件邮箱
     * @param authCode     授权码
     * @param notifyEmail  收件邮箱
     * @param userName     目标用户名
     * @param postTime     发言时间
     */
    public void sendMonitorAlert(String key, String emailAccount, String authCode,
                                String notifyEmail, String userName, String postTime) {
        String title = "论坛监控提醒";
        String content = String.format("发现目标用户 **%s** 有新发言\\n\\n" +
            "发言时间: %s\\n" +
            "来源: 同花顺论坛", userName, postTime);

        String emailContent = String.format("发现目标用户 %s 有新发言\\n\\n" +
            "发言时间: %s\\n" +
            "来源: 同花顺论坛", userName, postTime);

        // 发送Server酱通知
        sendServerChan(key, title, content);

        // 发送邮件通知
        sendEmail(emailAccount, authCode, notifyEmail, title, emailContent);
    }
}
