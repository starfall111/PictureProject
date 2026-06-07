package org.example.server.service.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.dto.seckill.SeckillMessage;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.entity.SeckillOrder;
import org.example.server.mapper.CodeCouponMapper;
import org.example.server.mapper.SeckillOrderMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Date;

/**
 * 秒杀订单 MQ 消费者
 * <p>
 * 核心流程:
 * 1. 幂等检查（DB 查重）
 * 2. 写秒杀订单（status=0 处理中）
 * 3. 分配编码券（FOR UPDATE SKIP LOCKED）
 * 4. 绑定用户到券
 * 5. 回填券 ID 到订单（status=1 成功）
 * 6. 异常时进入死信队列
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
    private StringRedisTemplate stringRedisTemplate;

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
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(SeckillMessage msg) {
        Long userId = msg.getUserId();
        Long batchId = msg.getBatchId();
        String orderNo = msg.getOrderNo();

        // 1. 幂等检查：是否已处理过该订单号
        LambdaQueryWrapper<SeckillOrder> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SeckillOrder::getOrderNo, orderNo);
        SeckillOrder existOrder = seckillOrderMapper.selectOne(existWrapper);
        if (existOrder != null && Integer.valueOf(1).equals(existOrder.getStatus())) {
            log.info("秒杀订单已处理，跳过 | orderNo={}", orderNo);
            return;
        }

        // 2. 写秒杀订单（status=0 处理中）
        SeckillOrder order = new SeckillOrder();
        order.setUserId(userId);
        order.setBatchId(batchId);
        order.setOrderNo(orderNo);
        order.setStatus(0);
        order.setCreateTime(new Date());
        if (existOrder == null) {
            seckillOrderMapper.insert(order);
        }

        // 3. 分配编码券 — 乐观查询批次下未发放的券
        LambdaQueryWrapper<CodeCoupon> couponWrapper = new LambdaQueryWrapper<>();
        couponWrapper.eq(CodeCoupon::getBatchId, batchId)
                .eq(CodeCoupon::getStatus, 0)
                .last("LIMIT 1");
        CodeCoupon coupon = codeCouponMapper.selectOne(couponWrapper);

        if (coupon == null) {
            // 无可用券，标记订单失败
            log.warn("批次无可用券，订单失败 | batchId={}, orderNo={}", batchId, orderNo);
            order.setStatus(2);
            seckillOrderMapper.updateById(order);
            return;
        }

        // 4. 绑定用户到券（使用乐观锁 version 防并发）
        LambdaUpdateWrapper<CodeCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CodeCoupon::getId, coupon.getId())
                .eq(CodeCoupon::getVersion, coupon.getVersion())
                .set(CodeCoupon::getUserId, userId)
                .set(CodeCoupon::getStatus, 1)
                .set(CodeCoupon::getIssuedAt, new Date());
        int rows = codeCouponMapper.update(null, updateWrapper);

        if (rows == 0) {
            // 乐观锁冲突，券已被其他消费者抢占
            log.warn("券分配冲突，订单失败 | couponId={}, orderNo={}", coupon.getId(), orderNo);
            order.setStatus(2);
            seckillOrderMapper.updateById(order);
            return;
        }

        // 5. 回填券 ID 到订单（status=1 成功）
        order.setCouponId(coupon.getId());
        order.setStatus(1);
        seckillOrderMapper.updateById(order);

        log.info("秒杀订单处理完成 | orderNo={}, couponId={}, userId={}", orderNo, coupon.getId(), userId);
    }
}
