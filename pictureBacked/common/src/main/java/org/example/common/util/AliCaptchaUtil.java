package util;

import com.aliyun.captcha20230305.Client;
import com.aliyun.captcha20230305.models.VerifyIntelligentCaptchaRequest;
import com.aliyun.captcha20230305.models.VerifyIntelligentCaptchaResponse;
import com.aliyun.captcha20230305.models.VerifyIntelligentCaptchaResponseBody;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliCaptchaUtil {

    @Value("${aliyun.captcha.sceneId}")
    private String sceneId;

    private Client createClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(System.getenv("OSS_ACCESS_KEY_ID"))
                .setAccessKeySecret(System.getenv("OSS_ACCESS_KEY_SECRET"));
        config.endpoint = "captcha.cn-shanghai.aliyuncs.com";
        return new Client(config);
    }

    /**
     * 校验阿里云验证码2.0
     *
     * @param captchaVerifyParam 前端传入的验证参数
     * @return 验证是否通过
     */
    public boolean checkCaptcha(String captchaVerifyParam) {
        try {
            Client client = createClient();

            VerifyIntelligentCaptchaRequest request = new VerifyIntelligentCaptchaRequest()
                    .setSceneId(sceneId)
                    .setCaptchaVerifyParam(captchaVerifyParam);

            VerifyIntelligentCaptchaResponse response = client.verifyIntelligentCaptcha(request);
            VerifyIntelligentCaptchaResponseBody body = response.getBody();

            log.info("验证码校验结果: requestId={}, code={}, message={}",
                    body.getRequestId(), body.getCode(), body.getMessage());

            if (body.getResult() != null) {
                return body.getResult().getVerifyResult();
            }
            return false;
        } catch (Exception e) {
            log.error("验证码校验异常", e);
            throw new RuntimeException("验证码校验失败", e);
        }
    }
}
