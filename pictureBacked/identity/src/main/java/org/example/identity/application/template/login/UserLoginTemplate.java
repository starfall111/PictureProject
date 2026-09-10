package org.example.identity.application.template.login;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.identity.interfaces.dto.UserLoginDTO;
import org.example.identity.interfaces.dto.UserRegisterDTO;
import org.example.identity.api.model.User;
import org.example.identity.infrastructure.persistence.UserMapper;
import org.example.identity.application.NoticeService;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;

public abstract class UserLoginTemplate {

    @Resource
    private UserMapper userMapper;

    @Resource
    private NoticeService noticeService;

    public final User login(UserLoginDTO userLoginDTO) {
        String account = userLoginDTO.getAccount();
        //1.校验格式 — 子类决定怎么校验
        validateFormat(account);

        //2.查找用户 — 公共实现，子类提供字段名
        User user = findUser(getQueryField(), account);

        //3.根据登录方式分支验证（后续有其他登录方式需改为策略模式）
        if (userLoginDTO.getIsVerityCode() == 0) {
            //密码登录
            ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.PARAMS_ERROR, "用户不存在");
            String encrypted = getEncryptPassword(userLoginDTO.getPassword());
            ThrowUtils.throwIf(!encrypted.equals(user.getUserPassword()), ErrorCode.PARAMS_ERROR, "账号或密码错误");
        } else if (userLoginDTO.getIsVerityCode() == 1) {
            //验证码登录
            if (ObjUtil.isEmpty(user)) {
                //用户不存在，自动注册 — 委托给子类配对的注册模板
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setAccount(userLoginDTO.getAccount());
                registerDTO.setType(userLoginDTO.getType());
                registerDTO.setVerityCode(userLoginDTO.getVerityCode());
                user = doAutoRegister(registerDTO);
            } else {
                noticeService.verityCode(userLoginDTO.getAccount(), userLoginDTO.getVerityCode());
            }
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return user;
    }

    // ========== 账号类型钩子（子类实现） ==========
    protected abstract void validateFormat(String account);
    protected abstract String getQueryField();

    // ========== 自动注册钩子（子类注入配对的注册模板） ==========
    protected abstract User doAutoRegister(UserRegisterDTO registerDTO);

    private User findUser(String field, String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(field, account);

        return userMapper.selectOne(queryWrapper);
    }

    private String getEncryptPassword(String password) {
        password = UserConstant.SALT + password;

        return DigestUtils.md5DigestAsHex(password.getBytes());
    }


}
