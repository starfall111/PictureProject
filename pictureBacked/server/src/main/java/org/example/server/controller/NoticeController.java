package org.example.server.controller;

import cn.hutool.core.util.ObjUtil;
import org.example.pojo.dto.user.SendVerificationCodeDTO;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.server.service.NoticeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/verification")
public class NoticeController {

    @Resource
    private NoticeService noticeService;

    @PostMapping("/send")
    public BaseResponse<String> sendVerificationCode(@RequestBody SendVerificationCodeDTO sendVerificationCodeDTO) throws ExecutionException, InterruptedException {
        ThrowUtils.throwIf(ObjUtil.isEmpty(sendVerificationCodeDTO), ErrorCode.PARAMS_ERROR);

        noticeService.sendCode(sendVerificationCodeDTO.getType(),sendVerificationCodeDTO.getAccount(),sendVerificationCodeDTO.getCaptchaVerifyParam());

        return ResultUtils.success("发送成功");
    }

    //验证

}
