package org.example.pictureBacked.controller;

import cn.hutool.core.util.ObjUtil;
import dto.SendVerificationCodeDTO;
import exception.ErrorCode;
import exception.ThrowUtils;
import org.example.pictureBacked.service.VerityCodeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import result.BaseResponse;
import result.ResultUtils;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/verification")
public class VerificationCodeController {

    @Resource
    private VerityCodeService verityCodeService;

    @PostMapping("/send")
    public BaseResponse<String> sendVerificationCode(SendVerificationCodeDTO sendVerificationCodeDTO) throws ExecutionException, InterruptedException {
        ThrowUtils.throwIf(ObjUtil.isEmpty(sendVerificationCodeDTO), ErrorCode.PARAMS_ERROR);

        verityCodeService.sendCode(sendVerificationCodeDTO.getType(),sendVerificationCodeDTO.getAccount(),sendVerificationCodeDTO.getCaptchaVerifyParam());

        return ResultUtils.success("发送成功");
    }

    //验证

}
