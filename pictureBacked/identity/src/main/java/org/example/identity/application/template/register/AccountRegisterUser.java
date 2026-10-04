package org.example.identity.application.template.register;

import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.identity.interfaces.dto.UserRegisterDTO;
import org.example.identity.api.model.User;
import org.springframework.stereotype.Service;

/**
 * @author Zou
 */
@Service
public class AccountRegisterUser extends UserRegisterTemplate{
    @Override
    protected void validateFormat(String account) {
        ThrowUtils.throwIf(account.length() < 4 || account.length() > 10, ErrorCode.PARAMS_ERROR,"账号长度不少于4位且不大于10位");
    }

    @Override
    protected String getQueryField() {
        return UserConstant.USER_ACCOUNT_FAILED;
    }

    @Override
    protected void validatePasswordOrVerificationCode(UserRegisterDTO userRegisterDTO) {
        String password = userRegisterDTO.getPassword();
        String checkPassword = userRegisterDTO.getCheckPassword();
        ThrowUtils.throwIf(password.length() < 6 || checkPassword.length() < 6, ErrorCode.PARAMS_ERROR, "密码过短");
        ThrowUtils.throwIf(!password.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
    }

    @Override
    protected User buildUser(UserRegisterDTO userRegisterDTO) {
        User user = new User();
        user.setUserAccount(userRegisterDTO.getAccount());
        user.setUserPassword(getEncryptPassword(userRegisterDTO.getPassword()));

        return user;
    }
}
