package org.example.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.pojo.dto.user.*;
import org.example.pojo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.vo.LoginUserVO;
import org.example.pojo.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 获取当前登录用户
     * */
    LoginUserVO getLoginUser();

    /**
     * 新增用户
     * */
    void addUser(UserAddDTO userAddDTO);


    /**
     * 更新用户信息()
     * */
    void updateUser(AdminUpdateDTO adminUpdateDTO);

    /**
     * 分页查询
     * */
    Page<UserVO> queryUserList(UserQueryDTO userQueryDTO);


}
