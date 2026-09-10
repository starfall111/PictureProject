package org.example.marketing.application.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 秒杀 HMAC 签名令牌工具
 * <p>
 * 生成和验证 userId:batchId:timestamp 的 HMAC-SHA256 签名，
 * 用于防止秒杀接口被恶意刷单。Token 有效期 5 分钟。
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Component
public class SeckillTokenUtil {

    @Value("${seckill.hmac-secret}")
    private String hmacSecret;

    private static final int TOKEN_TTL_SECONDS = 300;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private Mac macInstance;

    @PostConstruct
    public void init() {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            macInstance = Mac.getInstance(HMAC_ALGORITHM);
            macInstance.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 初始化失败", e);
        }
    }

    /**
     * 生成秒杀令牌
     *
     * @param userId  用户ID
     * @param batchId 批次ID
     * @return Base64UrlSafe(payload).Base64UrlSafe(signature)
     */
    public String generate(Long userId, Long batchId) {
        long timestamp = System.currentTimeMillis() / 1000;
        String payload = userId + ":" + batchId + ":" + timestamp;
        String signature = hmacSha256Hex(payload);
        return base64UrlEncode(payload) + "." + base64UrlEncode(signature);
    }

    /**
     * 验证秒杀令牌
     *
     * @param token 令牌字符串
     * @return [userId, batchId, timestamp]
     */
    public String[] verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求无效，请刷新页面重试");
        }
        String payload = base64UrlDecode(parts[0]);
        String signature = base64UrlDecode(parts[1]);
        String expected = hmacSha256Hex(payload);
        if (!expected.equals(signature)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求无效，请刷新页面重试");
        }
        String[] fields = payload.split(":");
        if (fields.length != 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求无效，请刷新页面重试");
        }
        long timestamp = Long.parseLong(fields[2]);
        long now = System.currentTimeMillis() / 1000;
        if (now - timestamp > TOKEN_TTL_SECONDS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Token 已过期，请重新获取");
        }
        return fields;
    }

    // ==================== 私有方法 ====================

    /**
     * HMAC-SHA256 签名（线程安全：每次 clone Mac 实例）
     */
    private String hmacSha256Hex(String data) {
        try {
            Mac mac = (Mac) macInstance.clone();
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64UrlDecode(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
