package org.example.server.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.annotation.CheckAuth;
import org.example.common.annotation.RateLimit;
import org.example.common.annotation.RateLimitDimension;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.user.*;
import org.example.pojo.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.example.common.constants.UserConstant;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.vo.UserProfileVO;
import org.example.pojo.vo.UserVO;
import org.example.server.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.vo.LoginUserVO;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;

/**
 * @author Zou
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource(name = "cachedUserService")
    private UserService cacheUserService;

    @PostMapping("/register")
    @RateLimit(resource = "register", dimensions = {RateLimitDimension.IP}, windowSeconds = 60, maxAttempts = 3)
    public BaseResponse<Long> register(@RequestBody UserRegisterDTO userRegisterDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userRegisterDTO), ErrorCode.PARAMS_ERROR);

        long result = cacheUserService.userRegister(userRegisterDTO);

        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    @RateLimit(resource = "login", dimensions = {RateLimitDimension.IP}, windowSeconds = 60, maxAttempts = 10)
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userLoginDTO),ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = cacheUserService.userLogin(userLoginDTO,request);

        return ResultUtils.success(loginUserVO);
    }

    //修改密码
    @PostMapping("/password/update")
    public BaseResponse<Boolean> updatePassword(@RequestBody UserPasswordUpdateDTO passwordUpdateDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(passwordUpdateDTO), ErrorCode.PARAMS_ERROR);

        cacheUserService.updatePassword(passwordUpdateDTO);

        return ResultUtils.success(true);
    }

    @GetMapping("/get/login")
//    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<LoginUserVO> getLoginUser(){
        return ResultUtils.success(cacheUserService.getLoginUser());
    }

    @PostMapping("/logout")
    public BaseResponse<String> logOut(HttpServletRequest request){
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user),ErrorCode.NOT_LOGIN_ERROR);

        UserContext.clear();
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        request.getSession().invalidate();

        return ResultUtils.success("注销成功");
    }

    //添加用户
    @PostMapping("/add")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<String> addUser(@RequestBody UserAddDTO userAddDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userAddDTO),ErrorCode.PARAMS_ERROR);
        cacheUserService.addUser(userAddDTO);

        return ResultUtils.success("ok");
    }
    //更新用户信息
    @PostMapping("/update")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 10)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDTO userUpdateDTO, HttpServletRequest request){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userUpdateDTO),ErrorCode.PARAMS_ERROR);

        User user = UserContext.get();
        User updateUser = new User();

        BeanUtil.copyProperties(userUpdateDTO,updateUser);
        updateUser.setId(user.getId());

        boolean result = cacheUserService.updateById(updateUser);

        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR);

        // 更新 Session 中的用户信息
        User latestUser = cacheUserService.getById(user.getId());
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, latestUser);

        return ResultUtils.success(true);

    }
    //更新用户信息（管理员）
    @PostMapping("/admin/update")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody AdminUpdateDTO adminUpdateDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(adminUpdateDTO),ErrorCode.PARAMS_ERROR);

        cacheUserService.updateUser(adminUpdateDTO);

        return ResultUtils.success(true);

    }
    //根据ID删除用户
    @DeleteMapping("/delete")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest),ErrorCode.PARAMS_ERROR);

        boolean result = cacheUserService.removeById(deleteRequest.getId());

        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);
    }
    //分页获取用户列表
    @PostMapping("/page/query")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByQuery(@RequestBody UserQueryDTO userQueryDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userQueryDTO),ErrorCode.PARAMS_ERROR);

        Page<UserVO> userVOPage = cacheUserService.queryUserList(userQueryDTO);

        return ResultUtils.success(userVOPage);
    }
    //根据ID获取用户信息（管理员）
    @GetMapping("get/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @RateLimit(dimensions = {RateLimitDimension.USER_OR_IP}, windowSeconds = 60, maxAttempts = 60)
    public BaseResponse<User> getUserInfo(@PathVariable Long id){
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);

        User user = cacheUserService.getById(id);

        ThrowUtils.throwIf(ObjUtil.isEmpty(user),ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(user);
    }
    //换绑手机号/邮箱
    @PostMapping("/bind/account")
    public BaseResponse<Boolean> bindAccount(@RequestBody UserBindAccountDTO userBindAccountDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userBindAccountDTO), ErrorCode.PARAMS_ERROR);

        cacheUserService.bindAccount(userBindAccountDTO);

        return ResultUtils.success(true);
    }

    //上传用户头像
    @PostMapping("/avatar/upload")
    @RateLimit(dimensions = {RateLimitDimension.USER}, windowSeconds = 60, maxAttempts = 5)
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        ThrowUtils.throwIf(ObjUtil.isEmpty(file), ErrorCode.PARAMS_ERROR, "文件不能为空");

        String url = cacheUserService.uploadAvatar(file);

        return ResultUtils.success(url);
    }

    //根据ID获取信息
    @GetMapping("/get/info")
    public BaseResponse<UserVO> getUserInfo(){
        User userlogin = UserContext.get();
        UserVO userVO = new UserVO();
        User user = cacheUserService.getById(userlogin.getId());
        BeanUtil.copyProperties(user,userVO);

        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户档案（含统计数据）
     */
    @GetMapping("/profile/{id}")
    public BaseResponse<UserProfileVO> getUserProfile(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        UserProfileVO profile = cacheUserService.getUserProfile(id);
        return ResultUtils.success(profile);
    }

    @GetMapping("/cache/get/login")
    public BaseResponse<LoginUserVO> getLoginUserCache(){
        return ResultUtils.success(cacheUserService.getLoginUser());
    }


    @GetMapping("/cache/profile/{id}")
    public BaseResponse<UserProfileVO> getUserProfileCache(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        UserProfileVO profile = cacheUserService.getUserProfile(id);
        return ResultUtils.success(profile);
    }

    //根据ID获取信息
    @GetMapping("/cache/get/info")
    public BaseResponse<UserVO> getUserInfoCache(){
        User userlogin = UserContext.get();
        UserVO userVO = new UserVO();
        User user = cacheUserService.getById(userlogin.getId());
        BeanUtil.copyProperties(user,userVO);

        return ResultUtils.success(userVO);
    }
}
