package org.example.identity.application.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.PictureConstant;
import org.example.shared.constants.RedisKeyConstants;
import org.example.identity.api.UserConstant;
import org.example.shared.contract.NotificationEvent;
import org.example.shared.contract.NotificationTypeEnum;
import org.example.identity.interfaces.dto.*;
import org.example.identity.api.model.User;
import org.example.identity.api.UserContext;
import org.example.identity.api.UserEnum;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.util.AliOssUtil;
import org.example.identity.api.VipUtil;
import org.example.identity.api.port.FollowStatsPort;
import org.example.identity.api.port.UserContentStatsPort;
import org.example.identity.api.port.model.UserContentStats;
import org.example.identity.interfaces.vo.UserProfileVO;
import org.example.identity.interfaces.vo.UserVO;
import org.example.identity.infrastructure.persistence.UserMapper;
import org.example.identity.application.NoticeService;
import org.example.identity.application.UserService;
import org.example.identity.application.template.login.AccountLoginUser;
import org.example.identity.application.template.login.EmailLoginUser;
import org.example.identity.application.template.login.PhoneLoginUser;
import org.example.identity.application.template.login.UserLoginTemplate;
import org.example.identity.api.event.BindPhoneEvent;
import org.example.identity.application.template.register.AccountRegisterUser;
import org.example.identity.application.template.register.EmailRegisterUser;
import org.example.identity.application.template.register.PhoneRegisterUser;
import org.example.identity.application.template.register.UserRegisterTemplate;
import org.example.shared.util.RateLimitUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.example.shared.util.SnowflakeIdWorker;
import org.example.identity.interfaces.vo.LoginUserVO;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Zou
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2026-05-11 20:54:16
 */
@Slf4j
@Service("dbUserService")
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private NoticeService noticeService;

    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    private AccountRegisterUser accountRegisterUser;

    @Resource
    private EmailRegisterUser emailRegisterUser;

    @Resource
    private PhoneRegisterUser phoneRegisterUser;

    @Resource
    private AccountLoginUser accountLoginUser;

    @Resource
    private EmailLoginUser emailLoginUser;

    @Resource
    private PhoneLoginUser phoneLoginUser;

    @Resource
    private RateLimitUtil rateLimitUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /** 用户内容统计端口（图片模块提供实现） */
    @Resource
    private UserContentStatsPort userContentStatsPort;

    /** 关注统计端口（图片模块·社交子域提供实现） */
    @Resource
    private FollowStatsPort followStatsPort;

    @Override
    public long userRegister(UserRegisterDTO userRegisterDTO) {
        //1.校验数据 和 判断注册类型
        Integer type = userRegisterDTO.getType();
        String account = userRegisterDTO.getAccount();
        //主体数据判空
        ThrowUtils.throwIf(ObjUtil.hasNull(type, account), ErrorCode.PARAMS_ERROR);

        UserRegisterTemplate userRegister = accountRegisterUser;

        //判断注册类型
        switch (type) {
            case 0:
                break;
            case 1:
                userRegister = phoneRegisterUser;
                break;
            case 2:
                userRegister = emailRegisterUser;
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据非法");
        }
        //2.入库
        User user = userRegister.register(userRegisterDTO);
        user.setUserName("默认昵称");
        user.setUserRole(UserEnum.USER.getValue());
        boolean result = this.save(user);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        // 发送欢迎通知（附带初始密码提醒）— 经发布语言事件解耦，由通知模块异步落
        try {
            eventPublisher.publishEvent(new NotificationEvent(
                    this, user.getId(), null, "系统管理员", null,
                    NotificationTypeEnum.SYSTEM,
                    "欢迎注册 PictureProject",
                    "感谢您的注册！您的初始密码为 123456，请尽快登录并修改密码以确保账号安全。",
                    null, null));
        } catch (Exception e) {
            log.warn("注册欢迎通知发送失败：userId={}", user.getId(), e);
        }

        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginDTO userLoginDTO, HttpServletRequest request) {

        //0.防撞库机制：滑动窗口限流
        //0.1 校验数据
        Integer type = userLoginDTO.getType();
        String account = userLoginDTO.getAccount();
        ThrowUtils.throwIf(ObjUtil.hasNull(type, account), ErrorCode.PARAMS_ERROR);

        //0.2 检查硬锁定（30 分钟窗口，连续触发后生效）
        RateLimitUtil.Result lockResult = rateLimitUtil.checkRateLimit(
                RedisKeyConstants.RATE_LIMIT_RESOURCE_LOGIN + ":lock", account,
                RedisKeyConstants.LOGIN_LOCK_WINDOW, 5);
        if (!lockResult.allowed()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "账号已被锁定，请30分钟后重试");
        }

        //0.3 滑动窗口限流（5 分钟内最多 5 次失败）
        RateLimitUtil.Result limitResult = rateLimitUtil.checkRateLimit(
                RedisKeyConstants.RATE_LIMIT_RESOURCE_LOGIN, account,
                RedisKeyConstants.LOGIN_RATE_LIMIT_WINDOW, RedisKeyConstants.LOGIN_RATE_LIMIT_MAX);
        if (!limitResult.allowed()) {
            // 触发锁定：设置 30 分钟硬锁
            rateLimitUtil.checkRateLimit(
                    RedisKeyConstants.RATE_LIMIT_RESOURCE_LOGIN + ":lock", account,
                    RedisKeyConstants.LOGIN_LOCK_WINDOW, 1);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "登录失败次数过多，账号已被锁定30分钟");
        }

        UserLoginTemplate userLogin = accountLoginUser;

        User user = new User();
        switch (type) {
            case 0:
                break;
            case 1:
                userLogin = phoneLoginUser;
                break;
            case 2:
                userLogin = emailLoginUser;
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法数据");
        }

        try {
            user = userLogin.login(userLoginDTO);
            //登录成功：清除限流窗口（重置计数器）
            clearLoginRateLimit(account);
        } catch (BusinessException e) {
            //登录失败：滑动窗口已自动记录，提示剩余次数
            int remaining = Math.max(0, RedisKeyConstants.LOGIN_RATE_LIMIT_MAX - 1);
            throw e;
        }

        //3.记录登录状态
        HttpSession session = request.getSession();
        session.setAttribute(UserConstant.USER_LOGIN_STATE, user);
        //4.返回LoginUserVO
        LoginUserVO userVO = new LoginUserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 登录成功后清除该账号的限流记录
     */
    private void clearLoginRateLimit(String account) {
        try {
            stringRedisTemplate.delete(
                    String.format("%s:%s:%s", RedisKeyConstants.RATE_LIMIT_KEY_PREFIX,
                            RedisKeyConstants.RATE_LIMIT_RESOURCE_LOGIN, account));
            stringRedisTemplate.delete(
                    String.format("%s:%s:%s", RedisKeyConstants.RATE_LIMIT_KEY_PREFIX,
                            RedisKeyConstants.RATE_LIMIT_RESOURCE_LOGIN + ":lock", account));
        } catch (Exception e) {
            log.warn("清除登录限流记录失败：account={}", account, e);
        }
    }

    @Override
    public LoginUserVO getLoginUser() {
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);

        user = this.getById(user.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public void addUser(UserAddDTO userAddDTO) {

        //防止用户重复
        User resultUser = verityAccount(UserConstant.USER_ACCOUNT_FAILED, userAddDTO.getUserAccount());

        ThrowUtils.throwIf(!ObjUtil.isEmpty(resultUser), ErrorCode.PARAMS_ERROR, "用户已存在");
        //构建入库user
        User user = new User();
        BeanUtil.copyProperties(userAddDTO, user);
        user.setUserPassword(getEncryptPassword("123456"));
        //入库
        boolean result = this.save(user);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
    }

    @Override
    public void updateUser(AdminUpdateDTO adminUpdateDTO) {
        //先找用户信息
        User user = this.getById(adminUpdateDTO.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.PARAMS_ERROR, "用户不存在");
        //更新用户信息
        user = new User();
        BeanUtil.copyProperties(adminUpdateDTO, user);
        boolean result = this.updateById(user);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
    }

    @Override
    public Page<UserVO> queryUserList(UserQueryDTO userQueryDTO) {

        //构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Long id = userQueryDTO.getId();
        String account = userQueryDTO.getUserAccount();
        String phone = userQueryDTO.getUserPhone();
        String email = userQueryDTO.getUserEmail();
        String name = userQueryDTO.getUserName();
        String role = userQueryDTO.getUserRole();
        String sortFiled = userQueryDTO.getSortField();
        String sortOrder = userQueryDTO.getSortOrder();

        queryWrapper.eq(!ObjUtil.isEmpty(id), "id", id);
        queryWrapper.eq(!ObjUtil.isEmpty(role), "userRole", role);
        queryWrapper.like(!ObjUtil.isEmpty(account), UserConstant.USER_ACCOUNT_FAILED, account);
        queryWrapper.like(!ObjUtil.isEmpty(phone), UserConstant.USER_PHONE_FAILED, phone);
        queryWrapper.like(!ObjUtil.isEmpty(email), UserConstant.USER_EMAIL_FAILED, email);
        queryWrapper.like(!ObjUtil.isEmpty(name), "userName", name);
        queryWrapper.orderBy(!ObjUtil.isEmpty(sortFiled), "ascend".equals(sortOrder), sortFiled);


        Integer current = userQueryDTO.getCurrent();
        Integer pageSize = userQueryDTO.getPageSize();

        //先找到所有符合条件的数据
        Page<User> userPage = this.page(new Page<>(current, pageSize), queryWrapper);

        //创建分页集准备存放转换过的UserVO列表
        Page<UserVO> userPageVO = new Page<>(current, pageSize, userPage.getTotal());

        //将User列表转变为 UserVO列表
        List<UserVO> userVOList = getUserVOList(userPage.getRecords());

        userPageVO.setRecords(userVOList);

        return userPageVO;
    }

    @Override
    public void updatePassword(UserPasswordUpdateDTO passwordUpdateDTO) {
        String oldPassword = passwordUpdateDTO.getOldPassword();
        String newPassword = passwordUpdateDTO.getNewPassword();
        String confirmPassword = passwordUpdateDTO.getConfirmPassword();

        //1.判空
        ThrowUtils.throwIf(StrUtil.hasBlank(oldPassword, newPassword, confirmPassword),
                ErrorCode.PARAMS_ERROR, "密码不能为空");
        //2.校验新密码与确认密码一致
        ThrowUtils.throwIf(!newPassword.equals(confirmPassword),
                ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        //3.校验新密码长度
        ThrowUtils.throwIf(newPassword.length() < 6,
                ErrorCode.PARAMS_ERROR, "密码长度不能小于6位");

        //4.获取当前用户
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        //5.校验旧密码
        String encryptOldPassword = getEncryptPassword(oldPassword);
        User dbUser = this.getById(currentUser.getId());
        ThrowUtils.throwIf(!encryptOldPassword.equals(dbUser.getUserPassword()),
                ErrorCode.PARAMS_ERROR, "原密码错误");

        //6.更新密码
        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setUserPassword(getEncryptPassword(newPassword));
        boolean result = this.updateById(updateUser);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
    }

    //密码加密
    private String getEncryptPassword(String password) {
        password = UserConstant.SALT + password;

        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    //抽象账号重复逻辑
    private long checkAccount(String filed, String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(filed, account);

        return this.baseMapper.selectCount(queryWrapper);
    }


    private User verityAccount(String filed, String account) {
        //校验基本信息
        ThrowUtils.throwIf(StrUtil.hasBlank(account), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(account.length() < 4, ErrorCode.PARAMS_ERROR, "账号错误");
        //先从数据库中找到对应数据
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(filed, account);
        //返回数据
        return this.baseMapper.selectOne(queryWrapper);
    }

    @Override
    public void bindAccount(UserBindAccountDTO userBindAccountDTO) {
        Integer type = userBindAccountDTO.getType();
        String account = userBindAccountDTO.getAccount();
        String verificationCode = userBindAccountDTO.getVerificationCode();

        //1.判空
        ThrowUtils.throwIf(ObjUtil.hasNull(type, account, verificationCode), ErrorCode.PARAMS_ERROR);

        //2.校验格式
        noticeService.checkPhoneOrEmail(type, account);

        //3.校验验证码
        noticeService.verityCode(account, verificationCode);

        //4.检查是否已被其他用户绑定
        String field = switch (type) {
            case 1 -> UserConstant.USER_PHONE_FAILED;
            case 2 -> UserConstant.USER_EMAIL_FAILED;
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据非法");
        };
        ThrowUtils.throwIf(checkAccount(field, account) > 0, ErrorCode.PARAMS_ERROR, "该账号已被绑定");

        //5.获取当前用户并更新
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        if (type == 1) {
            updateUser.setUserPhone(account);
        } else {
            updateUser.setUserEmail(account);
        }

        boolean result = this.updateById(updateUser);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        // 绑定手机号时触发自动过审事件
        if (type == 1) {
            eventPublisher.publishEvent(new BindPhoneEvent(this, currentUser.getId()));
        }
    }

    @Override
    public String uploadAvatar(MultipartFile file) throws Exception {
        //1.判空
        ThrowUtils.throwIf(ObjUtil.isEmpty(file), ErrorCode.PARAMS_ERROR, "文件不能为空");

        //2.校验文件格式
        String fileName = file.getOriginalFilename();
        ThrowUtils.throwIf(StrUtil.hasBlank(fileName), ErrorCode.PARAMS_ERROR, "文件名不能为空");
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        cn.hutool.core.util.ArrayUtil.contains(PictureConstant.IMAGE_TYPE_LIST, ext);
        ThrowUtils.throwIf(!cn.hutool.core.util.ArrayUtil.contains(PictureConstant.IMAGE_TYPE_LIST, ext),
                ErrorCode.PARAMS_ERROR, "不支持的图片格式");

        //3.获取当前用户
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        //4.上传文件到OSS
        String url = aliOssUtil.upload(file.getBytes(), fileName);
        try {
            //5.删除旧头像
            User oldUser = this.getById(currentUser.getId());
            if (StrUtil.isNotBlank(oldUser.getUserAvatar())) {
                aliOssUtil.deleteByUrl(oldUser.getUserAvatar());
            }

            //6.更新用户头像链接
            User updateUser = new User();
            updateUser.setId(currentUser.getId());
            updateUser.setUserAvatar(url);
            boolean result = this.updateById(updateUser);
            ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

            return url;
        } catch (Exception e) {
            //上传失败，删除已上传的文件
            aliOssUtil.deleteByUrl(url);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }
    }

    private UserVO getUserVO(User user) {
        if (ObjUtil.isEmpty(user)) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);

        return userVO;
    }

    private List<UserVO> getUserVOList(List<User> userList) {
        if (ObjUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }

        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        // 1. 查询用户基本信息
        User user = this.getById(userId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setUserName(user.getUserName());
        profile.setUserAvatar(user.getUserAvatar());
        profile.setUserProfile(user.getUserProfile());
        profile.setUserRole(user.getUserRole());
        profile.setVipType(user.getVipType());
        profile.setVipExpireTime(user.getVipExpireTime());
        profile.setIsActiveVip(VipUtil.isActiveVip(user));
        profile.setCreateTime(user.getCreateTime());

        // 2.~6. 经端口聚合内容统计（图片/社交数据所有权在图片模块，防腐层获取）
        UserContentStats stats = userContentStatsPort.getUserContentStats(userId);
        profile.setUserLikeCount(stats.getUserLikeCount());
        profile.setUserFavoriteCount(stats.getUserFavoriteCount());
        profile.setUploadCount(stats.getUploadCount());
        profile.setTotalLikes(stats.getTotalLikes());
        profile.setTotalFavorites(stats.getTotalFavorites());
        profile.setTotalViews(stats.getTotalViews());
        profile.setTotalShares(stats.getTotalShares());
        profile.setTotalDownloads(stats.getTotalDownloads());
        profile.setCategories(stats.getCategories());

        // 7. 查询关注数据
        org.example.identity.api.port.model.FollowCount followCount = followStatsPort.getFollowCount(userId);
        profile.setFollowCount(followCount.getFollowCount());
        profile.setFollowerCount(followCount.getFollowerCount());

        // 8. 查询当前登录用户是否关注了该用户
        try {
            User currentUser = UserContext.get();
            if (currentUser != null) {
                boolean isFollowed = followStatsPort.isFollowing(currentUser.getId(), userId);
                profile.setIsFollowed(isFollowed);
            } else {
                profile.setIsFollowed(false);
            }
        } catch (Exception e) {
            log.warn("查询关注状态失败: targetUserId={}", userId, e);
            profile.setIsFollowed(false);
        }

        return profile;
    }
}
