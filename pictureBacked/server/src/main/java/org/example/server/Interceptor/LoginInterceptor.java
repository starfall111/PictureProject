package org.example.server.Interceptor;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.constants.UserConstant;
import org.example.common.context.UserContext;
import org.example.pojo.entity.User;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.server.service.BanService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Zou
 * 登录拦截器
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BanService banService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (ObjUtil.hasEmpty(session, session.getAttribute(UserConstant.USER_LOGIN_STATE))) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        User user = (User) session.getAttribute(UserConstant.USER_LOGIN_STATE);

        // 封禁检查（Redis → DB 降级）
        checkBanStatus(user);

        UserContext.set(user);

        return true;
    }

    /**
     * 检查用户封禁状态
     * 优先查 Redis 缓存，未命中则查数据库并回写缓存
     * Redis 异常时降级查数据库
     */
    private void checkBanStatus(User user) {
        String banStatus = user.getBanStatus();

        // 快速路径：用户实体中 banStatus 为 NONE 直接放行
        if ("NONE".equals(banStatus) || ObjUtil.isEmpty(banStatus)) {
            return;
        }

        // 尝试从 Redis 获取最新封禁状态
        try {
            String banKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, user.getId());
            String cachedStatus = stringRedisTemplate.opsForValue().get(banKey);

            if (cachedStatus != null) {
                // 缓存命中，解析状态
                checkBanFromCache(cachedStatus, user.getId());
                return;
            }

            // 缓存未命中，查数据库获取最新状态
            banService.checkBanStatus(user);

        } catch (Exception e) {
            // Redis 异常降级：直接查数据库
            log.warn("Redis 封禁状态查询异常，降级查数据库，userId={}", user.getId(), e);
            banService.checkBanStatus(user);
        }
    }

    /**
     * 从缓存数据检查封禁状态
     */
    private void checkBanFromCache(String cachedStatus, Long userId) {
        // 缓存格式: NONE / TEMP:banEndTime / PERMANENT
        if ("NONE".equals(cachedStatus)) {
            return;
        }

        if ("PERMANENT".equals(cachedStatus)) {
            throw new BusinessException(ErrorCode.USER_BANNED_PERMANENT);
        }

        if (cachedStatus.startsWith("TEMP:")) {
            String endTimeStr = cachedStatus.substring(5);
            try {
                long endTime = Long.parseLong(endTimeStr);
                if (System.currentTimeMillis() > endTime) {
                    // 封禁已过期，清除缓存并放行
                    String banKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, userId);
                    stringRedisTemplate.delete(banKey);
                    return;
                }
                throw new BusinessException(ErrorCode.USER_BANNED);
            } catch (NumberFormatException e) {
                log.warn("封禁状态缓存格式异常，userId={}, value={}", userId, cachedStatus);
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear(); // 必须清理，防止内存泄漏
    }
}
