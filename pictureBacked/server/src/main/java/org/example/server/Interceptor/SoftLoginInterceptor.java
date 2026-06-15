package org.example.server.Interceptor;

import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.UserConstant;
import org.example.common.context.UserContext;
import org.example.pojo.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 软鉴权拦截器
 *
 * <p>针对允许游客访问、但已登录用户需要个性化数据（如点赞/收藏状态）的公共接口。
 * 尝试从 session 读取 user 并设置到 {@link UserContext}，未登录时不抛异常、不阻断请求。</p>
 *
 * <p>注意：与 {@link LoginInterceptor} 互斥使用——同一路径只会被其中一个拦截。
 * LoginInterceptor 强制登录且会做封禁检查；本拦截器仅填充上下文，不做任何鉴权。</p>
 *
 * @author Zou
 */
@Slf4j
@Component
public class SoftLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 尝试读取 session（不创建新 session）
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }
        Object loginUser = session.getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser instanceof User) {
            UserContext.set((User) loginUser);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
