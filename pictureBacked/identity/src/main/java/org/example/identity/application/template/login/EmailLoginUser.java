package org.example.identity.application.template.login;

import cn.hutool.core.util.ReUtil;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.identity.interfaces.dto.UserRegisterDTO;
import org.example.identity.api.model.User;
import org.example.identity.application.template.register.EmailRegisterUser;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class EmailLoginUser extends UserLoginTemplate{

    @Resource
    private EmailRegisterUser emailRegisterUser;

    @Override
    protected void validateFormat(String account) {
        ThrowUtils.throwIf(!ReUtil.isMatch("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", account), ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
    }

    @Override
    protected String getQueryField() {
        return UserConstant.USER_EMAIL_FAILED;
    }

    @Override
    protected User doAutoRegister(UserRegisterDTO registerDTO) {
        return emailRegisterUser.registerAndSave(registerDTO);
    }
}
