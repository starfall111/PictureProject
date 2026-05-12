package org.example.server.Interceptor;

import cn.hutool.core.util.ObjUtil;
import org.example.common.constants.UserConstant;
import org.example.common.context.UserContext;
import entity.User;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Zou
 * 登录拦截器
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler){

        HttpSession session = request.getSession(false);
        if(ObjUtil.hasEmpty(session,session.getAttribute(UserConstant.USER_LOGIN_STATE))){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        User user = (User) session.getAttribute(UserConstant.USER_LOGIN_STATE);
        UserContext.set(user);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear(); // 必须清理，防止内存泄漏
    }
}
