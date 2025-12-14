package com.zr.bili.service.impl;

import com.zr.bili.exception.RedisEmailEqual;
import com.zr.bili.exception.redisEmailEmpty;
import com.zr.bili.properties.EmailProperties;
import com.zr.bili.properties.SendEmailProperties;
import com.zr.bili.service.EmailService;
import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailProperties emailProperties;

    private final SendEmailProperties sendEmailProperties;

    private final StringRedisTemplate stringRedisTemplate;

    private final JavaMailSender javaMailSender;
    @Override
    public boolean sendCodeEmail(String toEmail, String code) {
        try {
            // 构建邮件
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(sendEmailProperties.getUsername());          // 发件人
            helper.setTo(toEmail);              // 收件人
            helper.setSubject(emailProperties.getSubject());    // 标题
            // 邮件内容（简单文本，避免被判定为垃圾邮件）
            String content = String.format("你的验证码为：%s，%d分钟内有效，请勿泄露给他人。", code, 5);
            helper.setText(content);

            // 发送邮件
            javaMailSender.send(mimeMessage);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 缓存验证码,先判断是否已经存在，若存在，返回否，controller返回报错信息“验证码已存在”
     * @param email
     * @param code
     * @return
     */
    @Override
    public boolean cacheCode(String email, String code) {
        if (!StringUtils.isEmpty(stringRedisTemplate.opsForValue().get(email))){
           throw new redisEmailEmpty("验证码已存在");
        }
        //缓存：以邮箱为key，验证码为value
        stringRedisTemplate.opsForValue().set(email,code,emailProperties.getExpire(), TimeUnit.SECONDS);
        return true;
    }

    /**
     * 验证码验证
     * @param email
     * @param code
     * @return
     */
    @Override
    public boolean verifyCode(String email, String code) {
        String cacheCode=stringRedisTemplate.opsForValue().get(email);
        if (cacheCode != null && !cacheCode.equals(code)) {
            throw new RedisEmailEqual("验证码错误");
        }
        stringRedisTemplate.delete(email);
        return true;
    }
}
