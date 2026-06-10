package org.example.server.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.CouponStatusEnum;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.server.mapper.CodeCouponBatchMapper;
import org.example.server.mapper.CodeCouponMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 编码券过期与对账定时任务
 * <p>
 * 包含：
 * 1. 过期券处理（每小时）- 将已领取但超期的券标记为已过期
 * 2. Redis-DB 库存对账（每10分钟）- 校准 Redis 库存与 DB 实际库存差异
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Component
public class CouponExpireScheduled {

    @Resource
    private CodeCouponMapper codeCouponMapper;

    @Resource
    private CodeCouponBatchMapper codeCouponBatchMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DefaultRedisScript<Long> seckillRollbackScript;

    /**
     * 过期券处理 — 每小时执行
     * <p>
     * 将状态为 CLAIMED 且超过有效期（发放后30天未激活）的券标记为 EXPIRED
     * </p>
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireCoupons() {
        String lockKey = "lock:coupon:expire";
        if (!tryLock(lockKey, 3500)) {
            log.info("过期券处理：另一个实例正在执行");
            return;
        }

        try {
            Date now = new Date();

            // 查询已领取但已过期的券（expireAt < now 且状态为 CLAIMED）
            // 这里以券自身的 expireAt 字段为准
            LambdaQueryWrapper<CodeCoupon> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CodeCoupon::getStatus, CouponStatusEnum.CLAIMED.getValue())
                    .lt(CodeCoupon::getExpireAt, now);

            // 注意：刚领取的券可能没有 expireAt，使用 issuedAt + 30天作为兜底
            // 已领取未使用的券 expireAt 为空时不算过期（需要等激活后才有过期时间）
            // 所以这里只处理有 expireAt 且已过期的券

            List<CodeCoupon> expiredCoupons = codeCouponMapper.selectList(wrapper);
            if (expiredCoupons.isEmpty()) {
                log.debug("过期券处理：无过期券");
                return;
            }

            log.info("过期券处理：发现 {} 张过期券", expiredCoupons.size());

            int successCount = 0;
            for (CodeCoupon coupon : expiredCoupons) {
                try {
                    LambdaUpdateWrapper<CodeCoupon> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(CodeCoupon::getId, coupon.getId())
                            .eq(CodeCoupon::getStatus, CouponStatusEnum.CLAIMED.getValue())
                            .set(CodeCoupon::getStatus, CouponStatusEnum.EXPIRED.getValue());
                    int rows = codeCouponMapper.update(null, updateWrapper);
                    if (rows > 0) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("过期券标记失败 | couponId={}", coupon.getId(), e);
                }
            }

            log.info("过期券处理完成 | total={}, success={}", expiredCoupons.size(), successCount);
        } catch (Exception e) {
            log.error("过期券处理任务执行失败", e);
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * Redis-DB 库存对账 — 每10分钟执行
     * <p>
     * 校准 Redis 中的库存与 DB 实际剩余库存的差异
     * 防止 Redis 库存与 DB 不一致导致超卖或少卖
     * </p>
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void reconcileStock() {
        String lockKey = RedisKeyConstants.LOCK_SECKILL_RECONCILE;
        if (!tryLock(lockKey, 500)) {
            log.info("库存对账：另一个实例正在执行");
            return;
        }

        try {
            // 查询所有进行中的批次（status=2）
            LambdaQueryWrapper<CodeCouponBatch> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CodeCouponBatch::getStatus, 2);
            List<CodeCouponBatch> activeBatches = codeCouponBatchMapper.selectList(wrapper);

            if (activeBatches.isEmpty()) {
                log.debug("库存对账：无进行中的批次");
                return;
            }

            int reconciledCount = 0;
            for (CodeCouponBatch batch : activeBatches) {
                try {
                    // 查询 DB 中实际剩余未发放的券数量
                    LambdaQueryWrapper<CodeCoupon> couponWrapper = new LambdaQueryWrapper<>();
                    couponWrapper.eq(CodeCoupon::getBatchId, batch.getId())
                            .eq(CodeCoupon::getStatus, CouponStatusEnum.UNSOLD.getValue());
                    Long actualStock = codeCouponMapper.selectCount(couponWrapper);

                    // 对比 Redis 库存（预热时写入的是 Hash 结构）
                    String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batch.getId());
                    Object redisStockObj = stringRedisTemplate.opsForHash().get(stockKey, "stock");

                    if (redisStockObj == null) {
                        // Redis 无库存数据，写入实际库存
                        stringRedisTemplate.opsForHash().put(stockKey, "stock", String.valueOf(actualStock));
                        log.info("库存对账：写入Redis库存 | batchId={}, stock={}", batch.getId(), actualStock);
                        reconciledCount++;
                        continue;
                    }

                    long redisStock = Long.parseLong(redisStockObj.toString());
                    if (redisStock != actualStock) {
                        // 差异超过阈值，以 DB 为准校准 Redis
                        log.warn("库存对账：发现差异 | batchId={}, redisStock={}, dbStock={}",
                                batch.getId(), redisStock, actualStock);
                        stringRedisTemplate.opsForHash().put(stockKey, "stock", String.valueOf(actualStock));
                        // 数据库回写库存
                        batch.setCurrentStock(actualStock.intValue());
                        codeCouponBatchMapper.updateById(batch);
                        reconciledCount++;
                    }
                } catch (Exception e) {
                    log.error("库存对账失败 | batchId={}", batch.getId(), e);
                }
            }

            if (reconciledCount > 0) {
                log.info("库存对账完成 | 检查批次数={}, 校准数={}", activeBatches.size(), reconciledCount);
            }
        } catch (Exception e) {
            log.error("库存对账任务执行失败", e);
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
