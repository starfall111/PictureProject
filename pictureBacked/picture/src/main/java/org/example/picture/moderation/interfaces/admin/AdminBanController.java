package org.example.picture.moderation.interfaces.admin;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.annotation.CheckAuth;
import org.example.identity.api.UserConstant;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.picture.moderation.interfaces.dto.BanUnbanDTO;
import org.example.picture.moderation.interfaces.vo.BanRecordVO;
import org.example.picture.moderation.application.BanService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 管理端 — 封禁管理 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/admin/ban")
public class AdminBanController {

    @Resource
    private BanService banService;

    /**
     * 获取封禁记录列表
     *
     * @param current    当前页码
     * @param pageSize   每页条数
     * @param banStatus  封禁状态筛选（可选）
     * @return 封禁记录分页列表
     */
    @GetMapping("/list")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<BanRecordVO>> getBanRecordList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String banStatus) {
        ThrowUtils.throwIf(current < 1 || pageSize < 1, ErrorCode.PARAMS_ERROR);

        Page<BanRecordVO> result = banService.getBanRecordList(current, pageSize, banStatus);
        return ResultUtils.success(result);
    }

    /**
     * 手动解封用户
     *
     * @param dto 解封参数（含用户 ID 和解封原因）
     * @return 操作结果
     */
    @PostMapping("/unban")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> unbanUser(@RequestBody BanUnbanDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getUserId()), ErrorCode.PARAMS_ERROR);

        banService.unbanUser(dto);
        return ResultUtils.success(true);
    }

    /**
     * 获取封禁统计数据
     *
     * @return 封禁统计信息
     */
    @GetMapping("/stats")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Map<String, Object>> getBanStats() {
        Map<String, Object> result = banService.getBanStats();
        return ResultUtils.success(result);
    }
}
