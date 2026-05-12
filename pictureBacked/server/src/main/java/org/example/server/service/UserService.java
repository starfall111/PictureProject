package org.example.pictureBacked.service;

import dto.UserLoginDTO;
import dto.UserRegisterDTO;
import entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import vo.LoginUserVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Zou
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-05-11 20:54:16
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册（三模式注册）
     * */
    long userRegister(UserRegisterDTO userRegisterDTO);

    /**
     * 用户登录
     * */
    LoginUserVO userLogin(UserLoginDTO userLoginDTO, HttpServletRequest request);
}
