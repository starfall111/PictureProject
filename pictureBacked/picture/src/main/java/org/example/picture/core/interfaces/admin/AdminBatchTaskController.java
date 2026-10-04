package org.example.picture.core.interfaces.admin;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.shared.annotation.CheckAuth;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.picture.core.interfaces.dto.BatchTaskQueryDTO;
import org.example.picture.core.interfaces.dto.BatchTaskUpdateDTO;
import org.example.picture.core.interfaces.vo.AdminBatchTaskVO;
import org.example.picture.core.interfaces.vo.BatchTaskStatsVO;
import org.example.picture.core.application.BatchTaskService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 管理端 — 批量任务管理 Controller
 *
 * @author Zou
 */
@RestController
@RequestMapping("/admin/batch/task")
public class AdminBatchTaskController {

    @Resource
    @Qualifier("dbBatchTaskService")
    private BatchTaskService batchTaskService;

    /**
     * 获取批量任务统计数据
     */
    @GetMapping("/stats")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<BatchTaskStatsVO> getAdminStats() {
        BatchTaskStatsVO result = batchTaskService.getAdminStats();
        return ResultUtils.success(result);
    }

    /**
     * 分页查询批量任务列表（含筛选）
     */
    @GetMapping("/list")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<AdminBatchTaskVO>> getAdminList(BatchTaskQueryDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);
        Page<AdminBatchTaskVO> result = batchTaskService.getAdminList(dto);
        return ResultUtils.success(result);
    }

    /**
     * 查询批量任务详情
     */
    @GetMapping("/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<AdminBatchTaskVO> getAdminDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        AdminBatchTaskVO result = batchTaskService.getAdminDetail(id);
        return ResultUtils.success(result);
    }

    /**
     * 逻辑删除批量任务
     */
    @DeleteMapping("/delete")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> adminDelete(@RequestBody Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        batchTaskService.adminDelete(id);
        return ResultUtils.success(true);
    }

    /**
     * 修改批量任务
     */
    @PostMapping("/update")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> adminUpdate(@RequestBody BatchTaskUpdateDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);
        batchTaskService.adminUpdate(dto);
        return ResultUtils.success(true);
    }
}
