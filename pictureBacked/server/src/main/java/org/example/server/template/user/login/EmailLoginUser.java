package org.example.server.template.user.login;

import cn.hutool.core.util.ReUtil;
import org.example.common.constants.UserConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.dto.user.UserRegisterDTO;
import org.example.pojo.entity.User;
import org.example.server.template.user.register.EmailRegisterUser;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
