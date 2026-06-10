package org.example.server.service.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.pojo.dto.seckill.SeckillMessage;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.pojo.entity.SeckillOrder;
import org.example.server.mapper.CodeCouponBatchMapper;
import org.example.server.mapper.CodeCouponMapper;
import org.example.server.mapper.SeckillOrderMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 秒杀订单 MQ 消费者
 * <p>
 * 核心流程（TransactionTemplate 编程式事务）:
 * 1. 事务外：幂等检查
 * 2. 事务内：扣减 DB 库存 → 行锁锁定并绑定券 → 创建订单
 * 3. 事务回滚时回滚 Redis 预扣库存 + 去重标记
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Component
public class SeckillConsumer {

    @Resource
    private SeckillOrderMapper seckillOrderMapper;

    @Resource
    private CodeCouponMapper codeCouponMapper;

    @Resource
    private CodeCouponBatchMapper codeCouponBatchMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource(name = "seckillRollbackScript")
    private DefaultRedisScript<Long> seckillRollbackScript;

    @RabbitListener(queues = "seckill.order.queue")
    public void handleSeckillOrder(String message, Channel channel,
                                   org.springframework.messaging.Message<String> msg) {
        long deliveryTag = 0;
        try {
            deliveryTag = (long) msg.getHeaders().get(AmqpHeaders.DELIVERY_TAG);

            SeckillMessage seckillMsg = JSONUtil.toBean(message, SeckillMessage.class);
            log.info("秒杀订单消费开始 | userId={}, batchId={}, orderNo={}",
                    seckillMsg.getUserId(), seckillMsg.getBatchId(), seckillMsg.getOrderNo());

            processOrder(seckillMsg);

            // 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("秒杀订单消费成功 | orderNo={}", seckillMsg.getOrderNo());
        } catch (Exception e) {
            log.error("秒杀订单消费失败，进入死信队列 | payload={}", message, e);
            try {
                // NACK 并不重试，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("NACK 失败", ex);
            }
        }
    }

    /**
     * 处理秒杀订单
     */
    public void processOrder(SeckillMessage msg) {
        Long userId = msg.getUserId();
        Long batchId = msg.getBatchId();
        String orderNo = msg.getOrderNo();

        // 1. 事务外 - 幂等检查
        SeckillOrder existOrder = checkIdempotent(orderNo);
        if (existOrder != null && Integer.valueOf(1).equals(existOrder.getStatus())) {
            log.info("秒杀订单已处理，跳过 | orderNo={}", orderNo);
            return;
        }

        // 2. 事务内 - 扣减DB库存 → 绑定券 → 创建订单
        try {
            transactionTemplate.execute(status -> {
                try {
                    doProcessInTransaction(msg, existOrder);
                    return true;
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw e;
                }
            });
        } catch (Exception e) {
            // 3. 事务回滚 → 回滚 Redis 预扣库存 + 去重标记
            log.error("秒杀事务回滚，回滚 Redis | orderNo={}", orderNo, e);
            rollbackRedis(userId, batchId);
            throw e;
        }
    }

    /**
     * 事务内核心处理：① 扣减 DB 库存 → ② 行锁锁定并绑定券 → ③ 创建订单
     */
    private void doProcessInTransaction(SeckillMessage msg, SeckillOrder existOrder) {
        Long userId = msg.getUserId();
        Long batchId = msg.getBatchId();
        String orderNo = msg.getOrderNo();

        // ① 扣减 DB 库存（currentStock > 0 作为乐观锁条件，单条 UPDATE 原子操作）
        LambdaUpdateWrapper<CodeCouponBatch> stockWrapper = new LambdaUpdateWrapper<>();
        stockWrapper.eq(CodeCouponBatch::getId, batchId)
                .gt(CodeCouponBatch::getCurrentStock, 0)
                .setSql("currentStock = currentStock - 1");
        int stockRows = codeCouponBatchMapper.update(null, stockWrapper);
        if (stockRows == 0) {
            throw new RuntimeException("库存不足 | batchId=" + batchId);
        }

        // ② 行锁锁定并绑定券给用户（FOR UPDATE SKIP LOCKED）
        CodeCoupon coupon = codeCouponMapper.selectOneAvailableForUpdate(batchId);
        if (coupon == null) {
            // 无可用券 → 抛异常回滚整个事务（库存也回滚）
            throw new RuntimeException("批次无可用券 | batchId=" + batchId);
        }

        // 直接更新券（行锁已保证独占，无需 version 校验）
        LambdaUpdateWrapper<CodeCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CodeCoupon::getId, coupon.getId())
                .set(CodeCoupon::getUserId, userId)
                .set(CodeCoupon::getStatus, 1)
                .set(CodeCoupon::getIssuedAt, new Date());
        codeCouponMapper.update(null, updateWrapper);

        // ③ 创建订单（status=1 成功，直接带 couponId）
        SeckillOrder order = new SeckillOrder();
        order.setUserId(userId);
        order.setBatchId(batchId);
        order.setOrderNo(orderNo);
        order.setCouponId(coupon.getId());
        order.setStatus(1);
        order.setCreateTime(new Date());
        if (existOrder == null) {
            seckillOrderMapper.insert(order);
        } else {
            order.setId(existOrder.getId());
            seckillOrderMapper.updateById(order);
        }

        log.info("秒杀订单处理完成 | orderNo={}, couponId={}, userId={}", orderNo, coupon.getId(), userId);
    }

    /**
     * 幂等检查：根据订单号查询是否已存在
     */
    private SeckillOrder checkIdempotent(String orderNo) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getOrderNo, orderNo);
        return seckillOrderMapper.selectOne(wrapper);
    }

    /**
     * 回滚 Redis 预扣库存 + 去重标记
     */
    private void rollbackRedis(Long userId, Long batchId) {
        // 回滚 Redis 预扣库存
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        try {
            stringRedisTemplate.execute(seckillRollbackScript, List.of(stockKey), "1");
        } catch (Exception ex) {
            log.error("【严重】回滚 Redis 库存失败，需人工介入 | batchId={}", batchId, ex);
        }

        // 回滚去重标记
        String dedupeKey = String.format(RedisKeyConstants.SECKILL_DEDUPE, userId, batchId);
        try {
            stringRedisTemplate.delete(dedupeKey);
        } catch (Exception ex) {
            log.error("【严重】回滚去重标记失败 | key={}", dedupeKey, ex);
        }
    }
}
