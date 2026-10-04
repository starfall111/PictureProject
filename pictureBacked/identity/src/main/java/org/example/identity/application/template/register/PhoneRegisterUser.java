package org.example.identity.application.template.register;

import cn.hutool.core.util.ReUtil;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.util.SnowflakeIdWorker;
import org.example.identity.interfaces.dto.UserRegisterDTO;
import org.example.identity.api.model.User;
import org.springframework.stereotype.Service;

@Service
public class PhoneRegisterUser extends UserRegisterTemplate{
    @Override
    protected void validateFormat(String account) {
        ThrowUtils.throwIf(!ReUtil.isMatch("^1[3-9]\\d{9}$", account), ErrorCode.PARAMS_ERROR, "账号格式不正确");
    }

    @Override
    protected String getQueryField() {
        return UserConstant.USER_PHONE_FAILED;
    }

    @Override
    protected void validatePasswordOrVerificationCode(UserRegisterDTO userRegisterDTO) {
        noticeService.verityCode(userRegisterDTO.getAccount(),userRegisterDTO.getVerityCode());
    }

    @Override
    protected User buildUser(UserRegisterDTO userRegisterDTO) {
        User user = new User();
        SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker(0,0);
        user.setUserAccount("user" + snowflakeIdWorker.nextId());
        user.setUserPhone(userRegisterDTO.getAccount());
        user.setUserPassword(getEncryptPassword(UserConstant.DEFAULT_PASSWORD));

        return user;
    }
}
