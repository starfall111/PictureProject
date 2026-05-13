package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReUtil;
import org.example.common.constants.UserConstant;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.AliCaptchaUtil;
import org.example.server.service.VerityCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.example.common.util.AliSMSUtil;
import org.example.common.util.EmailUtil;

import javax.annotation.Resource;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Zou
 */
@Service
public class VerityCodeServiceImpl implements VerityCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerityCodeServiceImpl.class);
    private final Random random = new Random();
    private static final int CODE_EXPIRE_SECONDS = 300;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EmailUtil emailUtil;

    @Resource
    private AliSMSUtil aliSMSUtil;

    @Resource
    private AliCaptchaUtil aliCaptchaUtil;


    @Override
    public void sendCode(Integer type,String account, String captchaVerifyParam) throws ExecutionException, InterruptedException {
        //1.校验账号格式是否正确
        checkPhoneOrEmail(type,account);
        //2.校验图形验证是否正确
        //TODO 后续前端集成图形验证后进行校验
        ThrowUtils.throwIf(!aliCaptchaUtil.checkCaptcha(captchaVerifyParam),ErrorCode.PARAMS_ERROR,"验证错误，请重试");
        //3.生成验证码
        // 生成 6 位验证码
        String code = String.format("%06d", random.nextInt(1000000));

        // 存入 Redis，5 分钟过期
        stringRedisTemplate.opsForValue().set(
                UserConstant.REDIS_VERITY_CODE_KEY + account,
                code,
                CODE_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );
        //4.发送验证码
        log.info("邮箱验证码发送成功: email={}, code={}", account, code);
        switch(type){
            case 1 -> {
//                aliSMSUtil.SMSSendCode(account,code,"100001",5);
            }
            case 2 ->{
                emailUtil.sendVerificationCode(account,code);
            }
        }

    }

    @Override
    public void checkPhoneOrEmail(Integer type, String account) {
        switch (type) {
            case 1:
                ThrowUtils.throwIf(!ReUtil.isMatch("^1[3-9]\\d{9}$", account), ErrorCode.PARAMS_ERROR, "手机号格式不正确");
                break;
            case 2:
                ThrowUtils.throwIf(!ReUtil.isMatch("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", account), ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
                break;
        }
    }

    @Override
    public void verityCode(String account, String code) {
        //拼接key值
        String key = UserConstant.REDIS_VERITY_CODE_KEY + account;
        String verityCode = stringRedisTemplate.opsForValue().get(key);

        //校验是否发送验证码
        ThrowUtils.throwIf(ObjUtil.isEmpty(verityCode), ErrorCode.PARAMS_ERROR, "请先发送验证码");

        //验证用户是否输入验证码
        ThrowUtils.throwIf(ObjUtil.isEmpty(code), ErrorCode.PARAMS_ERROR, "验证码不能为空");

        //校验验证码是否相等
        ThrowUtils.throwIf(!verityCode.equals(code), ErrorCode.PARAMS_ERROR, "验证码错误");

        //清除验证码
        stringRedisTemplate.delete(key);

    }
}
