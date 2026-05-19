package org.example.server.template.user.register;

import cn.hutool.core.util.ReUtil;
import org.example.common.constants.UserConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.SnowflakeIdWorker;
import org.example.pojo.dto.user.UserRegisterDTO;
import org.example.pojo.entity.User;
import org.springframework.stereotype.Service;

@Service
public class EmailRegisterUser extends UserRegisterTemplate{
    @Override
    protected void validateFormat(String account) {
        ThrowUtils.throwIf(!ReUtil.isMatch("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", account), ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
    }

    @Override
    protected String getQueryField() {
        return UserConstant.USER_EMAIL_FAILED;
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
        user.setUserEmail(userRegisterDTO.getAccount());
        user.setUserPassword(getEncryptPassword(UserConstant.DEFAULT_PASSWORD));
        return user;

    }
}
