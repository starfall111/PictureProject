package org.example.pictureBacked.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import constants.UserConstant;
import dto.UserLoginDTO;
import dto.UserRegisterDTO;
import entity.User;
import enums.UserEnum;
import exception.BusinessException;
import exception.ErrorCode;
import exception.ThrowUtils;
import org.example.pictureBacked.service.UserService;
import org.example.pictureBacked.mapper.UserMapper;
import org.example.pictureBacked.service.VerityCodeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import util.SnowflakeIdWorker;
import vo.LoginUserVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

    @Override
    public long userRegister(UserRegisterDTO userRegisterDTO) {
        //1.校验数据 和 判断注册类型
        Integer type = userRegisterDTO.getType();
        String account = userRegisterDTO.getAccount();
        //主体数据判空
        ThrowUtils.throwIf(ObjUtil.hasNull(userRegisterDTO, type, account), ErrorCode.PARAMS_ERROR);
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
                ThrowUtils.throwIf(password.length() < 8 || userRegisterDTO.getCheckPassword().length() < 8, ErrorCode.PARAMS_ERROR, "密码过短");
                ThrowUtils.throwIf(!password.equals(userRegisterDTO.getCheckPassword()), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
                checkAccount("userAccount", account);
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
                ThrowUtils.throwIf(checkAccount("userPhone", account) > 0, ErrorCode.PARAMS_ERROR, "账号重复");
                ;
                verityCodeService.verityCode(account, userRegisterDTO.getVerityCode());
                user.setUserPhone(account);
                user.setUserAccount(generateAccount());
                user.setUserPassword(getEncryptPassword("123456"));
                break;
            case 2:
                verityCodeService.checkPhoneOrEmail(2, account);
                ThrowUtils.throwIf(checkAccount("userEmail", account) > 0, ErrorCode.PARAMS_ERROR, "账号重复");
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

        ThrowUtils.throwIf(result, ErrorCode.SYSTEM_ERROR);
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginDTO userLoginDTO, HttpServletRequest request) {

        //TODO 需要预留防撞库机制

        //1.校验数据 数据结构不为空、type不为空、account不为空
        Integer type = userLoginDTO.getType();
        String account = userLoginDTO.getAccount();
        //主体数据判空
        ThrowUtils.throwIf(ObjUtil.hasNull(userLoginDTO, type, account), ErrorCode.PARAMS_ERROR);

        //2.根据登录类型进行判定 账号/手机号/邮箱 + 密码登录 or 手机号/邮箱 + 验证码注册 根据 isVerityCode字段走不同的方法
        Integer isVerityCode = userLoginDTO.getIsVerityCode();
        String password = userLoginDTO.getPassword();
        User user = new User();
        //注： 抽象出校验格式方法
        //2.1密码登录 根据account从数据库找到数据，进行常规对比
        if (isVerityCode == 0) {
            switch (type) {
                case 0:
                    user = verityAccount("userAccount", account);
                    ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.PARAMS_ERROR, "用户不存在");
                    password = getEncryptPassword(password);
                    ThrowUtils.throwIf(!password.equals(user.getUserPassword()), ErrorCode.PARAMS_ERROR, "账号或密码错误");
                    break;
                case 1:
                    verityCodeService.checkPhoneOrEmail(1, account);
                    user = verityAccount("userPhone", account);
                    ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.PARAMS_ERROR, "用户不存在");
                    password = getEncryptPassword(password);
                    ThrowUtils.throwIf(!password.equals(user.getUserPassword()), ErrorCode.PARAMS_ERROR, "账号或密码错误");
                    break;
                case 2:
                    verityCodeService.checkPhoneOrEmail(2, account);
                    user = verityAccount("userEmail", account);
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
                    yield verityAccount("userPhone", account);
                }
                case 2 -> {
                    verityCodeService.checkPhoneOrEmail(2, account);
                    yield verityAccount("userEmail", account);
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
        session.setAttribute("userId",user.getId());
        //4.返回LoginUserVO数据结构
        LoginUserVO userVO = new LoginUserVO();
        BeanUtil.copyProperties(user,userVO);
        return userVO;
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
}
