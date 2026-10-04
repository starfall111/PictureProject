package org.example.identity.application.template.login;

import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.identity.interfaces.dto.UserRegisterDTO;
import org.example.identity.api.model.User;
import org.example.identity.application.template.register.AccountRegisterUser;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class AccountLoginUser extends UserLoginTemplate{

    @Resource
    private AccountRegisterUser accountRegisterUser;

    @Override
    protected void validateFormat(String account) {
        ThrowUtils.throwIf(account.length() < 4, ErrorCode.PARAMS_ERROR,"账号格式不正确");
    }

    @Override
    protected String getQueryField() {
        return UserConstant.USER_ACCOUNT_FAILED;
    }

    @Override
    protected User doAutoRegister(UserRegisterDTO registerDTO) {
        return accountRegisterUser.registerAndSave(registerDTO);
    }
}
