package org.example.server.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.bytebuddy.implementation.bytecode.Throw;
import org.example.common.annotation.CheckAuth;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.user.*;
import org.example.pojo.entity.User;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Result;

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
    public BaseResponse<Long> register(@RequestBody UserRegisterDTO userRegisterDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userRegisterDTO), ErrorCode.PARAMS_ERROR);

        long result = userService.userRegister(userRegisterDTO);

        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userLoginDTO),ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userService.userLogin(userLoginDTO,request);

        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
//    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<LoginUserVO> getLoginUser(){
        return ResultUtils.success(userService.getLoginUser());
    }

    @PostMapping("/logout")
    public BaseResponse<String> logOut(HttpServletRequest request){
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user),ErrorCode.NOT_LOGIN_ERROR);

        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);

        return ResultUtils.success("注销成功");
    }

    //添加用户
    @PostMapping("/add")
    public BaseResponse<String> addUser(@RequestBody UserAddDTO userAddDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userAddDTO),ErrorCode.PARAMS_ERROR);
        userService.addUser(userAddDTO);

        return ResultUtils.success("ok");
    }
    //更新用户信息
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDTO userUpdateDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userUpdateDTO),ErrorCode.PARAMS_ERROR);

        User user = UserContext.get();
        User updateUser = new User();

        BeanUtil.copyProperties(userUpdateDTO,updateUser);
        updateUser.setId(user.getId());

        boolean result = userService.updateById(updateUser);

        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);

    }
    //更新用户信息（管理员）
    @PostMapping("/admin/update")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody AdminUpdateDTO adminUpdateDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(adminUpdateDTO),ErrorCode.PARAMS_ERROR);

        userService.updateUser(adminUpdateDTO);

        return ResultUtils.success(true);

    }
    //根据ID删除用户
    @DeleteMapping("/delete")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest),ErrorCode.PARAMS_ERROR);

        boolean result = userService.removeById(deleteRequest.getId());

        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(true);
    }
    //分页获取用户列表
    @PostMapping("/page/query")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByQuery(@RequestBody UserQueryDTO userQueryDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(userQueryDTO),ErrorCode.PARAMS_ERROR);

        Page<UserVO> userVOPage = userService.queryUserList(userQueryDTO);

        return ResultUtils.success(userVOPage);
    }
    //根据ID获取用户信息（管理员）
    @GetMapping("get/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<User> getUserInfo(@PathVariable Long id){
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);

        User user = userService.getById(id);

        ThrowUtils.throwIf(ObjUtil.isEmpty(user),ErrorCode.PARAMS_ERROR);

        return ResultUtils.success(user);
    }
    //换绑手机号/邮箱
    @PostMapping("/bind/account")
    public BaseResponse<Boolean> bindAccount(@RequestBody UserBindAccountDTO userBindAccountDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userBindAccountDTO), ErrorCode.PARAMS_ERROR);

        userService.bindAccount(userBindAccountDTO);

        return ResultUtils.success(true);
    }

    //上传用户头像
    @PostMapping("/avatar/upload")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws Exception {
        ThrowUtils.throwIf(ObjUtil.isEmpty(file), ErrorCode.PARAMS_ERROR, "文件不能为空");

        String url = userService.uploadAvatar(file);

        return ResultUtils.success(url);
    }

    //根据ID获取信息
    @GetMapping("/get/info")
    public BaseResponse<UserVO> getUserInfo(){
        User userlogin = UserContext.get();
        UserVO userVO = new UserVO();
        User user = userService.getById(userlogin.getId());
        BeanUtil.copyProperties(user,userVO);

        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户档案（含统计数据）
     */
    @GetMapping("/profile/{id}")
    public BaseResponse<UserProfileVO> getUserProfile(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        UserProfileVO profile = userService.getUserProfile(id);
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
