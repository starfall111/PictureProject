package org.example.identity.application.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.identity.api.UserContext;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.util.RedisCacheUtil;
import org.example.identity.interfaces.dto.*;
import org.example.identity.api.model.User;
import org.example.identity.interfaces.vo.LoginUserVO;
import org.example.identity.interfaces.vo.UserProfileVO;
import org.example.identity.interfaces.vo.UserVO;
import org.example.identity.infrastructure.persistence.UserMapper;
import org.example.identity.application.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * 缓存版用户服务实现
 * - 用户基本信息 → Redis 缓存，TTL 10-15min
 * - 用户档案统计 → Redis 缓存，TTL 5-8min
 * - 写操作 → 委托 dbUserService + 主动失效缓存
 *
 * @author Zou
 */
@Slf4j
@Service("cachedUserService")
public class CachedUserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    @Qualifier("dbUserService")
    private UserService dbUserService;

    // ==================== 缓存读方法 ====================

    @Override
    public LoginUserVO getLoginUser() {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        Long userId = currentUser.getId();
        String cacheKey = String.format(RedisKeyConstants.USER_INFO_KEY, userId);
        int ttlSeconds = 600 + RandomUtil.randomInt(0, 300);

        String json = redisCacheUtil.getWithLock(cacheKey, ttlSeconds, () -> {
            LoginUserVO vo = dbUserService.getLoginUser();
            return vo != null ? JSONUtil.toJsonStr(vo) : null;
        });

        return json != null ? JSONUtil.toBean(json, LoginUserVO.class) : null;
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        String cacheKey = String.format(RedisKeyConstants.USER_PROFILE_KEY, userId);
        int ttlSeconds = 300 + RandomUtil.randomInt(0, 180);

        String json = redisCacheUtil.getWithLock(cacheKey, ttlSeconds, () -> {
            UserProfileVO vo = dbUserService.getUserProfile(userId);
            return vo != null ? JSONUtil.toJsonStr(vo) : null;
        });

        return json != null ? JSONUtil.toBean(json, UserProfileVO.class) : null;
    }

    // ==================== 写方法（委托 + 缓存失效）====================

    @Override
    public boolean updateById(User entity) {
        boolean result = super.updateById(entity);
        if (result) {
            invalidateUserCache(entity.getId());
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);
        if (result) {
            invalidateUserCache((Long) id);
        }
        return result;
    }

    @Override
    public String uploadAvatar(MultipartFile file) throws Exception {
        String url = dbUserService.uploadAvatar(file);

        User currentUser = UserContext.get();
        invalidateUserCache(currentUser.getId());

        return url;
    }

    @Override
    public void updateUser(AdminUpdateDTO adminUpdateDTO) {
        dbUserService.updateUser(adminUpdateDTO);
        invalidateUserCache(adminUpdateDTO.getId());
    }

    @Override
    public void bindAccount(UserBindAccountDTO userBindAccountDTO) {
        dbUserService.bindAccount(userBindAccountDTO);

        User currentUser = UserContext.get();
        invalidateUserCache(currentUser.getId());
    }

    @Override
    public void updatePassword(UserPasswordUpdateDTO passwordUpdateDTO) {
        dbUserService.updatePassword(passwordUpdateDTO);
    }

    // ==================== 委托方法（无缓存）====================

    @Override
    public Page<UserVO> queryUserList(UserQueryDTO userQueryDTO) {
        return dbUserService.queryUserList(userQueryDTO);
    }

    @Override
    public long userRegister(UserRegisterDTO userRegisterDTO) {
        return dbUserService.userRegister(userRegisterDTO);
    }

    @Override
    public LoginUserVO userLogin(UserLoginDTO userLoginDTO, HttpServletRequest request) {
        return dbUserService.userLogin(userLoginDTO, request);
    }

    @Override
    public void addUser(UserAddDTO userAddDTO) {
        dbUserService.addUser(userAddDTO);
    }

    // ==================== 缓存失效方法 ====================

    private void invalidateUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        String infoKey = String.format(RedisKeyConstants.USER_INFO_KEY, userId);
        String profileKey = String.format(RedisKeyConstants.USER_PROFILE_KEY, userId);
        stringRedisTemplate.delete(infoKey);
        stringRedisTemplate.delete(profileKey);
        log.debug("Invalidated user cache for userId: {}", userId);
    }
}
