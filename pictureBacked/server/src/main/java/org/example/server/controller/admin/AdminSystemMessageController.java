package org.example.server.controller.admin;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.system_message.SystemMessageCreateDTO;
import org.example.pojo.dto.system_message.SystemMessageQueryDTO;
import org.example.pojo.entity.User;
import org.example.pojo.vo.SystemMessageVO;
import org.example.server.service.SystemMessageService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理员 — 系统消息管理 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/admin/system-message")
public class AdminSystemMessageController {

    @Resource
    private SystemMessageService systemMessageService;

    /**
     * 创建系统消息（草稿）
     */
    @PostMapping
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Long> createSystemMessage(@RequestBody SystemMessageCreateDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);

        User admin = UserContext.get();
        Long id = systemMessageService.createSystemMessage(dto, admin.getId());
        return ResultUtils.success(id);
    }

    /**
     * 编辑系统消息
     */
    @PutMapping
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> updateSystemMessage(@RequestBody SystemMessageCreateDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto) || ObjUtil.isEmpty(dto.getId()), ErrorCode.PARAMS_ERROR);

        boolean result = systemMessageService.updateSystemMessage(dto.getId(), dto);
        return ResultUtils.success(result);
    }

    /**
     * 删除系统消息
     */
    @DeleteMapping("/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> deleteSystemMessage(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        boolean result = systemMessageService.deleteSystemMessage(id);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询系统消息列表
     */
    @GetMapping("/list")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<SystemMessageVO>> listSystemMessages(SystemMessageQueryDTO dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);

        Page<SystemMessageVO> page = systemMessageService.listSystemMessages(dto);
        return ResultUtils.success(page);
    }

    /**
     * 发布系统消息
     */
    @PostMapping("/publish/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> publishSystemMessage(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        boolean result = systemMessageService.publishSystemMessage(id);
        return ResultUtils.success(result);
    }

    /**
     * 下架系统消息
     */
    @PostMapping("/revoke/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> revokeSystemMessage(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        boolean result = systemMessageService.revokeSystemMessage(id);
        return ResultUtils.success(result);
    }
}
