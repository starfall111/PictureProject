package org.example.server.controller.admin;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.enums.CouponStatusEnum;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.seckill.BatchCreateDTO;
import org.example.pojo.dto.seckill.BatchUpdateDTO;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.pojo.vo.seckill.SeckillStatsVO;
import org.example.server.mapper.CodeCouponBatchMapper;
import org.example.server.mapper.CodeCouponMapper;
import org.example.server.service.BatchManageService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 管理后台 — 批次管理
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/admin/batch")
public class AdminBatchController {

    @Resource
    private BatchManageService batchManageService;

    @Resource
    private CodeCouponBatchMapper batchMapper;

    @Resource
    private CodeCouponMapper couponMapper;

    /**
     * 查看批次详情
     */
    @GetMapping("/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<CodeCouponBatch> getBatchDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        CodeCouponBatch batch = batchMapper.selectById(id);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        return ResultUtils.success(batch);
    }

    /**
     * 创建发放批次
     */
    @PostMapping("/create")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Long> createBatch(@RequestBody BatchCreateDTO dto) {
        Long batchId = batchManageService.createBatch(dto);
        return ResultUtils.success(batchId);
    }

    /**
     * 批次列表
     */
    @GetMapping("/list")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Page<CodeCouponBatch>> listBatches(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<CodeCouponBatch> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CodeCouponBatch::getStatus, status);
        }
        wrapper.orderByDesc(CodeCouponBatch::getCreateTime);
        Page<CodeCouponBatch> page = batchMapper.selectPage(new Page<>(current, pageSize), wrapper);
        return ResultUtils.success(page);
    }

    /**
     * 生成兑换码（草稿阶段，幂等）
     */
    @PostMapping("/{id}/generate-codes")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> generateCodes(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        batchManageService.generateCodes(id);
        return ResultUtils.success(true);
    }

    /**
     * 修改批次数据（仅草稿阶段）
     */
    @PutMapping("/{id}")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> updateBatch(@PathVariable Long id, @RequestBody BatchUpdateDTO dto) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        dto.setId(id);
        batchManageService.updateBatch(dto);
        return ResultUtils.success(true);
    }

    /**
     * 统一状态流转
     * <p>
     * targetStatus: 1-预热中, 2-进行中, 3-已结束
     * </p>
     */
    @PostMapping("/{id}/transition")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> transition(@PathVariable Long id, @RequestParam int targetStatus) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        batchManageService.transitionBatch(id, targetStatus);
        return ResultUtils.success(true);
    }

    /**
     * 取消批次（仅草稿和预热中状态）
     */
    @PostMapping("/{id}/cancel")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        batchManageService.cancelBatch(id);
        return ResultUtils.success(true);
    }

    /**
     * 恢复已取消批次为草稿
     */
    @PostMapping("/{id}/restore")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> restore(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        batchManageService.restoreBatch(id);
        return ResultUtils.success(true);
    }

    /**
     * 结束批次（进行中 -> 已结束）
     */
    @PostMapping("/{id}/end")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> end(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        batchManageService.endBatch(id);
        return ResultUtils.success(true);
    }

    /**
     * 秒杀数据统计看板
     */
    @GetMapping("/stats")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<SeckillStatsVO> getSeckillStats() {
        SeckillStatsVO vo = new SeckillStatsVO();

        // 总批次数
        vo.setTotalBatches(Math.toIntExact(batchMapper.selectCount(null)));

        // 总券数、各状态券数
        vo.setTotalCoupons(Math.toIntExact(couponMapper.selectCount(null)));
        vo.setClaimedCount(Math.toIntExact(couponMapper.selectCount(
                new LambdaQueryWrapper<CodeCoupon>()
                        .gt(CodeCoupon::getStatus, CouponStatusEnum.UNSOLD.getValue())
        )));
        vo.setActivatedCount(Math.toIntExact(couponMapper.selectCount(
                new LambdaQueryWrapper<CodeCoupon>()
                        .eq(CodeCoupon::getStatus, CouponStatusEnum.ACTIVATED.getValue())
        )));
        vo.setExpiredCount(Math.toIntExact(couponMapper.selectCount(
                new LambdaQueryWrapper<CodeCoupon>()
                        .eq(CodeCoupon::getStatus, CouponStatusEnum.EXPIRED.getValue())
        )));

        // 领取率 = 已领取(含使用/过期) / 总券数
        if (vo.getTotalCoupons() > 0) {
            vo.setClaimRate((double) vo.getClaimedCount() / vo.getTotalCoupons());
        }
        // 激活率 = 已激活 / 已领取
        if (vo.getClaimedCount() > 0) {
            vo.setActivationRate((double) vo.getActivatedCount() / vo.getClaimedCount());
        }

        return ResultUtils.success(vo);
    }
}
