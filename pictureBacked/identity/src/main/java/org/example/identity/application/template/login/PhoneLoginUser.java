package org.example.identity.application.template.login;

import cn.hutool.core.util.ReUtil;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.identity.interfaces.dto.UserRegisterDTO;
import org.example.identity.api.model.User;
import org.example.identity.application.template.register.PhoneRegisterUser;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class PhoneLoginUser extends UserLoginTemplate{

    @Resource
    private PhoneRegisterUser phoneRegisterUser;

    @Override
    protected void validateFormat(String account) {
        ThrowUtils.throwIf(!ReUtil.isMatch("^1[3-9]\\d{9}$", account), ErrorCode.PARAMS_ERROR, "账号格式不正确");
    }

    @Override
    protected String getQueryField() {
        return UserConstant.USER_PHONE_FAILED;
    }

    @Override
    protected User doAutoRegister(UserRegisterDTO registerDTO) {
        return phoneRegisterUser.registerAndSave(registerDTO);
    }
}
