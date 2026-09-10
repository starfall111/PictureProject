package org.example.identity.interfaces;

import cn.hutool.core.util.ObjUtil;
import org.example.shared.annotation.RateLimit;
import org.example.shared.annotation.RateLimitDimension;
import org.example.identity.interfaces.dto.SendVerificationCodeDTO;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.identity.application.NoticeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;

import jakarta.annotation.Resource;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/verification")
public class NoticeController {

    @Resource
    private NoticeService noticeService;

    @PostMapping("/send")
    @RateLimit(resource = "notice.verify", dimensions = {RateLimitDimension.IP}, windowSeconds = 60, maxAttempts = 1)
    public BaseResponse<String> sendVerificationCode(@RequestBody SendVerificationCodeDTO sendVerificationCodeDTO) throws ExecutionException, InterruptedException {
        ThrowUtils.throwIf(ObjUtil.isEmpty(sendVerificationCodeDTO), ErrorCode.PARAMS_ERROR);

        noticeService.sendCode(sendVerificationCodeDTO.getType(),sendVerificationCodeDTO.getAccount(),sendVerificationCodeDTO.getCaptchaVerifyParam());

        return ResultUtils.success("发送成功");
    }

    //验证

}
