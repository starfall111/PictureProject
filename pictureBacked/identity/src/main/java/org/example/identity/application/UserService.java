package org.example.identity.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.identity.interfaces.dto.*;
import org.example.identity.api.model.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.identity.interfaces.vo.LoginUserVO;
import org.example.identity.interfaces.vo.UserProfileVO;
import org.example.identity.interfaces.vo.UserVO;

import jakarta.servlet.http.HttpServletRequest;

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

    /**
     * 换绑手机号/邮箱
     * */
    void bindAccount(UserBindAccountDTO userBindAccountDTO);

    /**
     * 上传用户头像
     * */
    String uploadAvatar(org.springframework.web.multipart.MultipartFile file) throws Exception;

    /**
     * 获取用户档案（含统计数据）
     *
     * @param userId 用户 ID
     * @return 用户档案 VO
     */
    UserProfileVO getUserProfile(Long userId);

    /**
     * 修改密码
     */
    void updatePassword(UserPasswordUpdateDTO passwordUpdateDTO);

}
