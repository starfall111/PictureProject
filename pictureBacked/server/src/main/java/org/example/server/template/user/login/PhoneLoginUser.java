package org.example.server.template.user.login;

import cn.hutool.core.util.ReUtil;
import org.example.common.constants.UserConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.dto.user.UserRegisterDTO;
import org.example.pojo.entity.User;
import org.example.server.template.user.register.PhoneRegisterUser;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
