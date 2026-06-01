package org.example.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.PictureConstant;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.constants.UserConstant;
import org.example.common.enums.NotificationTypeEnum;
import org.example.pojo.dto.user.*;
import org.example.pojo.dto.social.UserPictureQueryDTO;
import org.example.pojo.entity.Category;
import org.example.pojo.entity.Notification;
import org.example.pojo.entity.Picture;
import org.example.pojo.entity.User;
import org.example.common.context.UserContext;
import org.example.common.enums.UserEnum;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.AliOssUtil;
import org.example.pojo.entity.PictureStatistics;
import org.example.pojo.vo.CategoryBriefVO;
import org.example.pojo.vo.UserProfileVO;
import org.example.pojo.vo.UserVO;
import org.example.server.mapper.PictureFavoriteMapper;
import org.example.server.mapper.PictureLikeMapper;
import org.example.server.mapper.PictureMapper;
import org.example.server.mapper.PictureStatisticsMapper;
import org.example.server.mapper.UserMapper;
import org.example.server.service.CategoryService;
import org.example.server.service.NoticeService;
import org.example.server.service.NotificationService;
import org.example.server.service.UserService;
import org.example.server.template.user.login.AccountLoginUser;
import org.example.server.template.user.login.EmailLoginUser;
import org.example.server.template.user.login.PhoneLoginUser;
import org.example.server.template.user.login.UserLoginTemplate;
import org.example.server.service.event.BindPhoneEvent;
import org.example.server.template.user.register.AccountRegisterUser;
import org.example.server.template.user.register.EmailRegisterUser;
import org.example.server.template.user.register.PhoneRegisterUser;
import org.example.server.template.user.register.UserRegisterTemplate;
import org.example.common.util.RateLimitUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.example.common.util.SnowflakeIdWorker;
import org.example.pojo.vo.LoginUserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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
    private PictureMapper pictureMapper;

    @Resource
    private PictureStatisticsMapper pictureStatisticsMapper;

    @Resource
    private CategoryService categoryService;

    @Resource
    private PictureLikeMapper pictureLikeMapper;

    @Resource
    private PictureFavoriteMapper pictureFavoriteMapper;

    @Resource
    private RateLimitUtil rateLimitUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource(name = "cachedNotificationService")
    private NotificationService notificationService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

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

        // 发送欢迎通知（附带初始密码提醒）
        try {
            Notification welcome = new Notification();
            welcome.setReceiverId(user.getId());
            welcome.setSenderId(null);
            welcome.setSenderName("系统管理员");
            welcome.setType(NotificationTypeEnum.SYSTEM.getType());
            welcome.setTitle("欢迎注册 PictureProject");
            welcome.setContent("感谢您的注册！您的初始密码为 123456，请尽快登录并修改密码以确保账号安全。");
            welcome.setIsRead(0);
            notificationService.save(welcome);
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
        profile.setCreateTime(user.getCreateTime());

        // 2. 统计用户点赞和收藏的图片数量
        UserPictureQueryDTO emptyQuery = new UserPictureQueryDTO();
        Long userLikeCount = pictureLikeMapper.countUserLikedPictures(userId, emptyQuery);
        Long userFavoriteCount = pictureFavoriteMapper.countUserFavoritedPictures(userId, emptyQuery);
        profile.setUserLikeCount(userLikeCount != null ? userLikeCount.intValue() : 0);
        profile.setUserFavoriteCount(userFavoriteCount != null ? userFavoriteCount.intValue() : 0);

        // 3. 统计公共图库上传数量
        QueryWrapper<Picture> pictureQw = new QueryWrapper<>();
        pictureQw.eq("userId", userId)
                .isNull("spaceId")
                .eq("reviewStatus", 1);
        Integer uploadCount = pictureMapper.selectCount(pictureQw).intValue();
        profile.setUploadCount(uploadCount);

        // 3. 如果没有公共图片，返回零统计数据
        if (uploadCount == 0) {
            profile.setTotalLikes(0);
            profile.setTotalFavorites(0);
            profile.setTotalViews(0);
            profile.setTotalShares(0);
            profile.setTotalDownloads(0);
            profile.setCategories(new ArrayList<>());
            return profile;
        }

        // 4. 获取用户所有公共图片 ID 列表
        List<Picture> pictures = pictureMapper.selectList(pictureQw);
        List<Long> pictureIds = pictures.stream()
                .map(Picture::getId)
                .collect(Collectors.toList());

        // 5. 查询这些图片的统计数据并聚合
        QueryWrapper<PictureStatistics> statsQw = new QueryWrapper<>();
        statsQw.in("pictureId", pictureIds);
        List<PictureStatistics> statisticsList = pictureStatisticsMapper.selectList(statsQw);

        int totalLikes = 0;
        int totalFavorites = 0;
        int totalViews = 0;
        int totalShares = 0;
        int totalDownloads = 0;

        for (PictureStatistics stat : statisticsList) {
            totalLikes += stat.getLikeCount() != null ? stat.getLikeCount() : 0;
            totalFavorites += stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0;
            totalViews += stat.getViewCount() != null ? stat.getViewCount() : 0;
            totalShares += stat.getShareCount() != null ? stat.getShareCount() : 0;
            totalDownloads += stat.getDownloadCount() != null ? stat.getDownloadCount() : 0;
        }

        profile.setTotalLikes(totalLikes);
        profile.setTotalFavorites(totalFavorites);
        profile.setTotalViews(totalViews);
        profile.setTotalShares(totalShares);
        profile.setTotalDownloads(totalDownloads);

        // 6. 查询用户图片涉及的分类
        QueryWrapper<Picture> categoryQw = new QueryWrapper<>();
        categoryQw.select("DISTINCT categoryId")
                .eq("userId", userId)
                .isNull("spaceId")
                .eq("reviewStatus", 1)
                .isNotNull("categoryId");
        List<Picture> categoryPictures = pictureMapper.selectList(categoryQw);

        List<Long> categoryIds = categoryPictures.stream()
                .map(Picture::getCategoryId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        if (categoryIds.isEmpty()) {
            profile.setCategories(new ArrayList<>());
            return profile;
        }

        // 批量获取分类信息
        List<Category> categories = categoryService.listByIds(categoryIds);
        List<CategoryBriefVO> categoryVOList = categories.stream()
                .map(cat -> {
                    CategoryBriefVO vo = new CategoryBriefVO();
                    vo.setId(cat.getId());
                    vo.setName(cat.getName());
                    return vo;
                })
                .collect(Collectors.toList());

        profile.setCategories(categoryVOList);

        return profile;
    }
}
