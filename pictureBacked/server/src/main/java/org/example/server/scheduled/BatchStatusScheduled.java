package org.example.server.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.server.mapper.CodeCouponBatchMapper;
import org.example.server.service.BatchManageService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 预热中 -> 进行中（每分钟扫描）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void activateBatches() {
        String lockKey = "lock:batch:status:activate";
        if (!tryLock(lockKey, 55)) {
            return;
        }

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
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 进行中 -> 已结束（每分钟扫描）
     */
    @Scheduled(cron = "30 * * * * ?")
    public void endBatches() {
        String lockKey = "lock:batch:status:end";
        if (!tryLock(lockKey, 55)) {
            return;
        }

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
        } finally {
            unlock(lockKey);
        }
    }

    private boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
