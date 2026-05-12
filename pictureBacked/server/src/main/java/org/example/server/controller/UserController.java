package org.example.pictureBacked.controller;


import cn.hutool.core.util.ObjUtil;
import dto.UserLoginDTO;
import dto.UserRegisterDTO;
import exception.ErrorCode;
import exception.ThrowUtils;
import org.example.pictureBacked.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import result.BaseResponse;
import result.ResultUtils;
import vo.LoginUserVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Zou
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterDTO userRegisterDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userRegisterDTO), ErrorCode.PARAMS_ERROR);

        long result = userService.userRegister(userRegisterDTO);

        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userLoginDTO),ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userService.userLogin(userLoginDTO,request);

        return ResultUtils.success(loginUserVO);
    }
}
