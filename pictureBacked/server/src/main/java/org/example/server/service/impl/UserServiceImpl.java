package org.example.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.common.constants.PictureConstant;
import org.example.common.constants.UserConstant;
import org.example.pojo.dto.user.*;
import org.example.pojo.entity.User;
import org.example.common.context.UserContext;
import org.example.common.enums.UserEnum;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.AliOssUtil;
import org.example.pojo.vo.UserVO;
import org.example.server.service.UserService;
import org.example.server.mapper.UserMapper;
import org.example.server.service.VerityCodeService;
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

//TODO DTO判空转交至Controller检查
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private VerityCodeService verityCodeService;

    @Resource
    private AliOssUtil aliOssUtil;

    @Override
    public long userRegister(UserRegisterDTO userRegisterDTO) {
        //1.校验数据 和 判断注册类型
        Integer type = userRegisterDTO.getType();
        String account = userRegisterDTO.getAccount();
        //主体数据判空
        ThrowUtils.throwIf(ObjUtil.hasNull(type, account), ErrorCode.PARAMS_ERROR);
        //检查用户名长度
        ThrowUtils.throwIf(account.length() < 4, ErrorCode.PARAMS_ERROR, "用户名过短");

        User user = new User();

        //判断注册类型
        switch (type) {
            case 0:
                //检查密码长度
                //校验密码是否一致
                //检查account是否重复
                //密码加密
                String password = userRegisterDTO.getPassword();
                ThrowUtils.throwIf(password.length() < 6 || userRegisterDTO.getCheckPassword().length() < 6, ErrorCode.PARAMS_ERROR, "密码过短");
                ThrowUtils.throwIf(!password.equals(userRegisterDTO.getCheckPassword()), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
                checkAccount(UserConstant.USER_ACCOUNT_FAILED, account);
                password = getEncryptPassword(password);
                user.setUserAccount(account);
                user.setUserPassword(password);
                break;
            //校验格式是否正确
            //校验是否重复注册
            //检验验证码
            //生成随机account填入数据库
            case 1:
                verityCodeService.checkPhoneOrEmail(1, account);
                ThrowUtils.throwIf(checkAccount(UserConstant.USER_PHONE_FAILED, account) > 0, ErrorCode.PARAMS_ERROR, "账号重复");
                ;
                verityCodeService.verityCode(account, userRegisterDTO.getVerityCode());
                user.setUserPhone(account);
                user.setUserAccount(generateAccount());
                user.setUserPassword(getEncryptPassword("123456"));
                break;
            case 2:
                verityCodeService.checkPhoneOrEmail(2, account);
                ThrowUtils.throwIf(checkAccount(UserConstant.USER_EMAIL_FAILED, account) > 0, ErrorCode.PARAMS_ERROR, "账号重复");
                ;
                verityCodeService.verityCode(account, userRegisterDTO.getVerityCode());
                user.setUserEmail(account);
                user.setUserAccount(generateAccount());
                user.setUserPassword(getEncryptPassword("123456"));
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据非法");
        }
        //2.入库
        user.setUserName("默认昵称");
        user.setUserRole(UserEnum.USER.getValue());
        boolean result = this.save(user);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginDTO userLoginDTO, HttpServletRequest request) {

        //TODO 需要预留防撞库机制

        //1.校验数据 数据结构不为空、type不为空、account不为空
        Integer type = userLoginDTO.getType();
        String account = userLoginDTO.getAccount();
        //主体数据判空
        ThrowUtils.throwIf(ObjUtil.hasNull(type, account), ErrorCode.PARAMS_ERROR);

        //2.根据登录类型进行判定 账号/手机号/邮箱 + 密码登录 or 手机号/邮箱 + 验证码注册 根据 isVerityCode字段走不同的方法
        Integer isVerityCode = userLoginDTO.getIsVerityCode();
        String password = userLoginDTO.getPassword();
        User user = new User();
        //注： 抽象出校验格式方法
        //2.1密码登录 根据account从数据库找到数据，进行常规对比
        if (isVerityCode == 0) {
            switch (type) {
                case 0:
                    user = verityAccount(UserConstant.USER_ACCOUNT_FAILED, account);
                    ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.PARAMS_ERROR, "用户不存在");
                    password = getEncryptPassword(password);
                    ThrowUtils.throwIf(!password.equals(user.getUserPassword()), ErrorCode.PARAMS_ERROR, "账号或密码错误");
                    break;
                case 1:
                    verityCodeService.checkPhoneOrEmail(1, account);
                    user = verityAccount(UserConstant.USER_PHONE_FAILED, account);
                    ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.PARAMS_ERROR, "用户不存在");
                    password = getEncryptPassword(password);
                    ThrowUtils.throwIf(!password.equals(user.getUserPassword()), ErrorCode.PARAMS_ERROR, "账号或密码错误");
                    break;
                case 2:
                    verityCodeService.checkPhoneOrEmail(2, account);
                    user = verityAccount(UserConstant.USER_EMAIL_FAILED, account);
                    ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.PARAMS_ERROR, "用户不存在");
                    password = getEncryptPassword(password);
                    ThrowUtils.throwIf(!password.equals(user.getUserPassword()), ErrorCode.PARAMS_ERROR, "账号或密码错误");
                    break;
                default:
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法数据");
            }
        }
        //2.2验证码登录 先检查数据库内是否存在，存在走正常的登录逻辑，不存在则自动注册账号
        else if (isVerityCode == 1) {
            //TODO 考虑拆解 verityAccount（不传 password） 直接返回找到的user 不管是不是空，null在验证码登录时直接去注册方法，其他的手动才处理抛出错误
            long count = 0;
            user = switch (type) {
                case 1 -> {
                    verityCodeService.checkPhoneOrEmail(1, account);
                    yield verityAccount(UserConstant.USER_PHONE_FAILED, account);
                }
                case 2 -> {
                    verityCodeService.checkPhoneOrEmail(2, account);
                    yield verityAccount(UserConstant.USER_EMAIL_FAILED, account);
                }

                default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法数据");
            };
            if (ObjUtil.isEmpty(user)) {
                long id = userRegister(new UserRegisterDTO(type, account, null, null, userLoginDTO.getVerityCode()));
                user = this.baseMapper.selectById(id);
            } else {
                verityCodeService.verityCode(account, userLoginDTO.getVerityCode());
            }
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法数据");
        }

        //3.记录登录状态 session记录
        HttpSession session = request.getSession();
        session.setAttribute(UserConstant.USER_LOGIN_STATE, user);
        //4.返回LoginUserVO数据结构
        LoginUserVO userVO = new LoginUserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
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

        ThrowUtils.throwIf(!ObjUtil.isEmpty(resultUser),ErrorCode.PARAMS_ERROR,"用户已存在");
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

    //密码加密
    private String getEncryptPassword(String password) {
        password = UserConstant.SALT + password;

        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    //生成随机account
    private String generateAccount() {
        SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker(0, 0);
        return "user" + snowflakeIdWorker.nextId();
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
        verityCodeService.checkPhoneOrEmail(type, account);

        //3.校验验证码
        verityCodeService.verityCode(account, verificationCode);

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
}
