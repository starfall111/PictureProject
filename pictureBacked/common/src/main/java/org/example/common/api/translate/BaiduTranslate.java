package org.example.common.api.translate;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 百度翻译 API 封装
 */
@Slf4j
@Component
public class BaiduTranslate {

    private static final String API_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";

    @Value("${baidu.translate.appid:}")
    private String appId;

    @Value("${baidu.translate.secret:}")
    private String secret;

    private final Random random = new Random();

    /**
     * 英文翻译为中文
     *
     * @param text 英文文本
     * @return 中文翻译结果，翻译失败时返回原文
     */
    public String translateEnToZh(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(secret)) {
            log.warn("百度翻译未配置 appid 或 secret，跳过翻译");
            return text;
        }

        try {
            String salt = String.valueOf(random.nextInt(100000));
            String signRaw = appId + text + salt + secret;
            String sign = DigestUtils.md5DigestAsHex(signRaw.getBytes(StandardCharsets.UTF_8));
            log.debug("百度翻译签名原文: {}, 签名结果: {}", signRaw, sign);

            String url = String.format("%s?q=%s&from=en&to=zh&appid=%s&salt=%s&sign=%s",
                    API_URL,
                    URLEncoder.encode(text, StandardCharsets.UTF_8),
                    appId,
                    salt,
                    sign
            );

            HttpResponse response = HttpRequest.get(url)
                    .timeout(5000)
                    .execute();

            if (HttpStatus.HTTP_OK != response.getStatus()) {
                log.error("百度翻译请求失败，状态码: {}", response.getStatus());
                return text;
            }

            String body = response.body();
            Map<String, Object> resultMap = JSONUtil.toBean(body, Map.class);

            // 检查错误码
            if (resultMap.containsKey("error_code")) {
                log.error("百度翻译错误: {} - {}", resultMap.get("error_code"), resultMap.get("error_msg"));
                return text;
            }

            List<Map<String, String>> transResult = (List<Map<String, String>>) resultMap.get("trans_result");
            if (transResult != null && !transResult.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, String> item : transResult) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(item.get("dst"));
                }
                return sb.toString();
            }

            return text;
        } catch (Exception e) {
            log.error("百度翻译异常", e);
            return text;
        }
    }
}
