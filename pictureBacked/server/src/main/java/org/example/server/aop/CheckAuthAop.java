package org.example.server.aop;

import org.example.pojo.entity.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.common.annotation.CheckAuth;
import org.example.common.context.UserContext;
import org.example.common.enums.UserEnum;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.server.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Zou
 */
@Aspect
@Component
public class CheckAuthAop {

    @Resource
    private UserService userService;

    @Around("@annotation(checkAuth)")
    public Object checkRoleInterceptor(ProceedingJoinPoint checkPoint, CheckAuth checkAuth) throws Throwable {
        //获取当前接口需要的权限
        String mustRole = checkAuth.mustRole();
        UserEnum mustRoleEnum = UserEnum.getByValue(mustRole);
        //获取当前登录用户
        User user = UserContext.get();
        user = userService.getById(user.getId());
        //判断权限是否满足
        UserEnum userEnum = UserEnum.getByValue(user.getUserRole());
        //管理员权限（当mustRole等于admin时且user的role不为admin时，拦截）
        ThrowUtils.throwIf(UserEnum.ADMIN.equals(mustRoleEnum) && !UserEnum.ADMIN.equals(userEnum), ErrorCode.NO_AUTH_ERROR);
        //VIP会员等等权限
        //放行
        return checkPoint.proceed();
    }
}
