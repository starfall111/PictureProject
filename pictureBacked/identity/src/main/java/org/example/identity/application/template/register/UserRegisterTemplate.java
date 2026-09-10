package org.example.identity.application.template.register;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.identity.api.UserEnum;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.identity.interfaces.dto.UserRegisterDTO;
import org.example.identity.api.model.User;
import org.example.identity.infrastructure.persistence.UserMapper;
import org.example.identity.application.NoticeService;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;

public abstract class UserRegisterTemplate {

    @Resource
    private UserMapper userMapper;

    @Resource
    protected NoticeService noticeService;


    /**
     * 构建用户实体（不入库），供 UserServiceImpl 正常注册使用
     */
    public final User register(UserRegisterDTO userRegisterDTO){
        //获取账号
        String account = userRegisterDTO.getAccount();
        //校验account格式
        validateFormat(account);

        String field = getQueryField();
        //查找数据库内是否有重复账号
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(field,account);

        User user = userMapper.selectOne(queryWrapper);

        ThrowUtils.throwIf(ObjUtil.isNotEmpty(user), ErrorCode.PARAMS_ERROR,"用户已存在");

        //校验两次密码或验证码是否正确
        validatePasswordOrVerificationCode(userRegisterDTO);

        //构建User实体类
        return buildUser(userRegisterDTO);
    }

    /**
     * 构建用户实体 + 设置默认字段 + 入库，供登录模板自动注册使用
     */
    public final User registerAndSave(UserRegisterDTO userRegisterDTO) {
        User user = register(userRegisterDTO);
        user.setUserName("默认昵称");
        user.setUserRole(UserEnum.USER.getValue());
        int result = userMapper.insert(user);
        ThrowUtils.throwIf(result <= 0, ErrorCode.SYSTEM_ERROR);
        return user;
    }


    protected String getEncryptPassword(String password) {
        password = UserConstant.SALT + password;

        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    protected abstract void validateFormat(String account);

    protected abstract String getQueryField();

    protected abstract void validatePasswordOrVerificationCode(UserRegisterDTO userRegisterDTO);

    protected abstract User buildUser(UserRegisterDTO userRegisterDTO);
}
