package org.example.server.template.user.login;

import org.example.common.constants.UserConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.dto.user.UserRegisterDTO;
import org.example.pojo.entity.User;
import org.example.server.template.user.register.AccountRegisterUser;
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
