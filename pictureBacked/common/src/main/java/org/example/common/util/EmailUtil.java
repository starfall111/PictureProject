package org.example.common.util;

import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

@Component
@Slf4j
public class EmailUtil {

    private static final String REDIS_KEY_PREFIX = "email_code:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;



    public void sendVerificationCode(String email,String code) {
        // 构建邮件内容
        String subject = "【Picture】邮箱验证码";
        String content = "<div style=\"padding:20px;font-family:sans-serif;\">"
                + "<h2>邮箱验证码</h2>"
                + "<p>您的验证码为：<strong style=\"font-size:24px;color:#165DFF;\">" + code + "</strong></p>"
                + "<p>验证码 5 分钟内有效，请勿泄露给他人。</p>"
                + "</div>";

        // 发送邮件
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Picture");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("邮件发送失败: email={}, error={}", email, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"邮件发送失败，请稍后重试");
        }
    }

    /**
     * 发送图片审批结果通知邮件
     */
    public void sendReviewNotice(String to, String pictureName, boolean passed, String reviewMessage) {
        String statusText = passed ? "已通过" : "未通过";
        String statusColor = passed ? "#00b42e" : "#f53f3f";
        String subject = "【Picture】图片审批" + statusText + " - " + pictureName;
        String content = "<div style=\"padding:20px;font-family:sans-serif;\">"
                + "<h2>图片审批通知</h2>"
                + "<p>您上传的图片 <strong>" + pictureName + "</strong> 审批结果：</p>"
                + "<p style=\"font-size:18px;color:" + statusColor + ";\"><strong>" + statusText + "</strong></p>"
                + "<p>审批意见：" + (cn.hutool.core.util.StrUtil.isNotBlank(reviewMessage) ? reviewMessage : "无") + "</p>"
                + "</div>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Picture");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("审批通知邮件发送失败: email={}, error={}", to, e.getMessage());
        }
    }
}
