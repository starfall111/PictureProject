package org.example.marketing.application.scheduling;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.marketing.domain.model.CodeCouponBatch;
import org.example.marketing.infrastructure.persistence.CodeCouponBatchMapper;
import org.example.marketing.application.BatchManageService;
import org.example.shared.util.RedisCacheUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 批次状态自动推进定时任务
 * <p>
 * 预热中(1) -> 进行中(2)：startTime <= NOW() 时自动推进
 * 进行中(2) -> 已结束(3)：endTime <= NOW() 时自动推进
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Component
public class BatchStatusScheduled {

    @Resource
    private CodeCouponBatchMapper batchMapper;

    @Resource
    private BatchManageService batchManageService;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    /**
     * 预热中 -> 进行中（每分钟扫描）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void activateBatches() {
        redisCacheUtil.tryExecuteWithLock("lock:batch:status:activate", () -> {
            try {
                LambdaQueryWrapper<CodeCouponBatch> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CodeCouponBatch::getStatus, 1)
                        .le(CodeCouponBatch::getStartTime, new Date());
                List<CodeCouponBatch> batches = batchMapper.selectList(wrapper);

                for (CodeCouponBatch batch : batches) {
                    try {
                        batchManageService.transitionBatch(batch.getId(), 2);
                        log.info("定时推进批次状态: 预热中->进行中 | batchId={}", batch.getId());
                    } catch (Exception e) {
                        log.error("定时推进批次状态失败 | batchId={}", batch.getId(), e);
                    }
                }
            } catch (Exception e) {
                log.error("批次激活定时任务执行失败", e);
            }
        });
    }

    /**
     * 进行中 -> 已结束（每分钟扫描）
     */
    @Scheduled(cron = "30 * * * * ?")
    public void endBatches() {
        redisCacheUtil.tryExecuteWithLock("lock:batch:status:end", () -> {
            try {
                LambdaQueryWrapper<CodeCouponBatch> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CodeCouponBatch::getStatus, 2)
                        .le(CodeCouponBatch::getEndTime, new Date());
                List<CodeCouponBatch> batches = batchMapper.selectList(wrapper);

                for (CodeCouponBatch batch : batches) {
                    try {
                        batchManageService.endBatch(batch.getId());
                        log.info("定时推进批次状态: 进行中->已结束 | batchId={}", batch.getId());
                    } catch (Exception e) {
                        log.error("定时结束批次失败 | batchId={}", batch.getId(), e);
                    }
                }
            } catch (Exception e) {
                log.error("批次结束定时任务执行失败", e);
            }
        });
    }
}
