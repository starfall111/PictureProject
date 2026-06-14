# Phase 3 — 秒杀核心流程

> 优先级：P0 | 依赖：Phase 1 | 预估工时：3d | 可与 Phase 2 并行

## 目标

实现秒杀抢购的完整链路：HMAC Token 签发 → 限流 → 幂等（签名校验 + Redis 去重） → Lua 扣库存 → MQ 异步 → 消费者写 DB → 结果查询 → WebSocket 推送。

---

## 1. SeckillTokenUtil — HMAC 签名工具

创建 `server/src/main/java/org/example/server/util/SeckillTokenUtil.java`：

```java
package org.example.server.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 秒杀 Token 工具 — HMAC-SHA256 无状态签名
 *
 * Token 格式: Base64URL(payload).Base64URL(signature)
 * Payload:    userId:batchId:timestamp(秒)
 * Signature:  HMAC-SHA256(payload, serverSecret)
 *
 * 优势：无需 Redis 存储，省去 Token 的 SET / GET / DEL 三次 Redis 操作。
 * 幂等性由 grab 流程中的 Redis dedupe（SET NX）+ MQ 消费者 DB 查重保证。
 */
@Slf4j
@Component
public class SeckillTokenUtil {

    /** 服务端签名密钥，通过 application.yml 配置 */
    @Value("${seckill.hmac-secret}")
    private String hmacSecret;

    /** Token 有效期（秒），默认 5 分钟 */
    private static final int TOKEN_TTL_SECONDS = 300;

    private final HMac hmac = SecureUtil.hmacSha256(hmacSecret.getBytes(StandardCharsets.UTF_8));

    /**
     * 生成签名 Token
     */
    public String generate(Long userId, Long batchId) {
        long timestamp = System.currentTimeMillis() / 1000;
        String payload = userId + ":" + batchId + ":" + timestamp;
        String signature = hmac.digestHex(payload);
        return Base64.encodeUrlSafe(payload)
                + "." + Base64.encodeUrlSafe(signature);
    }

    /**
     * 验证并解析 Token，返回 payload 数组 [userId, batchId, timestamp]
     * 校验失败直接抛异常
     */
    public String[] verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求无效，请刷新页面重试");
        }

        String payload = Base64.decodeStr(parts[0]);
        String signature = Base64.decodeStr(parts[1]);

        // 1. 验签
        String expected = hmac.digestHex(payload);
        if (!expected.equals(signature)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求无效，请刷新页面重试");
        }

        // 2. 解析 payload
        String[] fields = payload.split(":");
        if (fields.length != 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求无效，请刷新页面重试");
        }

        // 3. 检查过期
        long timestamp = Long.parseLong(fields[2]);
        long now = System.currentTimeMillis() / 1000;
        if (now - timestamp > TOKEN_TTL_SECONDS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Token 已过期，请重新获取");
        }

        return fields; // [userId, batchId, timestamp]
    }
}
```

### 1.1 application.yml 追加配置

```yaml
seckill:
  hmac-secret: ${SECKILL_HMAC_SECRET:your-random-secret-key-at-least-32-chars}
```

> 生产环境通过环境变量 `SECKILL_HMAC_SECRET` 注入，不要硬编码。

---

## 2. SeckillController

创建 `server/src/main/java/org/example/server/controller/SeckillController.java`：

```java
package org.example.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.context.UserContext;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.seckill.SeckillGrabDTO;
import org.example.pojo.entity.SeckillOrder;
import org.example.server.service.SeckillService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀抢购接口
 */
@Slf4j
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    /**
     * 获取秒杀 Token（HMAC 签名，无状态）
     * GET /api/seckill/token?batchId=1
     */
    @GetMapping("/token")
    @CheckAuth
    public BaseResponse<Map<String, Object>> getToken(
            @RequestParam Long batchId,
            HttpServletRequest request) {
        Long userId = UserContext.get().getId();
        String token = seckillService.getToken(userId, batchId);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expireIn", 300);
        return ResultUtils.success(data);
    }

    /**
     * 秒杀抢购
     * POST /api/seckill/grab
     */
    @PostMapping("/grab")
    @CheckAuth
    public BaseResponse<Map<String, Object>> grab(
            @RequestBody SeckillGrabDTO dto,
            HttpServletRequest request) {
        Long userId = UserContext.get().getId();
        String clientIP = getClientIP(request);

        String orderNo = seckillService.grab(userId, dto.getBatchId(), dto.getToken(), clientIP);

        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", "PENDING");
        return ResultUtils.success(data);
    }

    /**
     * 查询抢购结果
     * GET /api/seckill/result?orderNo=xxx
     */
    @GetMapping("/result")
    @CheckAuth
    public BaseResponse<SeckillOrder> getResult(@RequestParam String orderNo) {
        Long userId = UserContext.get().getId();
        SeckillOrder order = seckillService.getResult(userId, orderNo);
        return ResultUtils.success(order);
    }

    /**
     * 获取批次信息（前端倒计时用）
     * GET /api/seckill/batch/{batchId}
     */
    @GetMapping("/batch/{batchId}")
    public BaseResponse<Map<String, Object>> getBatchInfo(@PathVariable Long batchId) {
        return ResultUtils.success(seckillService.getBatchInfo(batchId));
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

### 2.1 SeckillGrabDTO

创建 `pojo/src/main/java/org/example/pojo/dto/seckill/SeckillGrabDTO.java`：

```java
package org.example.pojo.dto.seckill;

import lombok.Data;

/**
 * 秒杀抢购请求
 */
@Data
public class SeckillGrabDTO {

    private Long batchId;

    private String token;
}
```

---

## 3. SeckillServiceImpl — 核心实现

创建 `server/src/main/java/org/example/server/service/impl/SeckillServiceImpl.java`：

```java
package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.util.RateLimitUtil;
import org.example.pojo.dto.seckill.SeckillMessage;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.pojo.entity.SeckillOrder;
import org.example.server.config.RabbitMQConfig;
import org.example.server.mapper.CodeCouponBatchMapper;
import org.example.server.mapper.SeckillOrderMapper;
import org.example.server.service.SeckillService;
import org.example.server.util.SeckillTokenUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现
 */
@Slf4j
@Service
public class SeckillServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder>
        implements SeckillService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private CodeCouponBatchMapper batchMapper;

    @Resource
    private RateLimitUtil rateLimitUtil;

    @Resource(name = "seckillDeductScript")
    private DefaultRedisScript<Long> seckillDeductScript;

    @Resource
    private SeckillTokenUtil seckillTokenUtil;

    @Override
    public String getToken(Long userId, Long batchId) {
        // 检查批次是否存在且有效
        checkBatchExists(batchId);

        // HMAC 签名生成 Token（无状态，无需 Redis 存储）
        return seckillTokenUtil.generate(userId, batchId);
    }

    @Override
    public String grab(Long userId, Long batchId, String token, String clientIP) {
        // 1. 参数校验
        if (ObjUtil.hasEmpty(batchId, token)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 降级检查（读 Redis 降级开关）
        String degradeLevel = stringRedisTemplate.opsForValue().get(RedisKeyConstants.SECKILL_DEGRADE);
        if (degradeLevel != null && Integer.parseInt(degradeLevel) >= 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "活动暂时不可用");
        }

        // 3. HMAC Token 验签（替代原 Redis GET+DELETE Token）
        //    验签 + 过期检查 + userId/batchId 绑定，一步完成
        String[] payload = seckillTokenUtil.verify(token);
        Long tokenUserId = Long.parseLong(payload[0]);
        Long tokenBatchId = Long.parseLong(payload[1]);
        if (!tokenUserId.equals(userId) || !tokenBatchId.equals(batchId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求无效，Token 与用户/批次不匹配");
        }

        // 4. 活动状态校验（读 Redis 预热数据）
        String infoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String batchInfoJson = stringRedisTemplate.opsForValue().get(infoKey);
        if (StrUtil.isBlank(batchInfoJson)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动不存在或未预热");
        }

        Map<String, Object> batchInfo = JSONUtil.toBean(batchInfoJson, Map.class);
        checkBatchTime(batchInfo);

        // 5. 限流校验（IP + 用户双维度）
        rateLimitUtil.checkRateLimit("seckill_ip", clientIP, 60, 10);
        rateLimitUtil.checkRateLimit("seckill_user", String.valueOf(userId), 3600, 3);

        // 6. 防重校验（Redis SET NX）— 核心幂等保障
        String dedupeKey = String.format(RedisKeyConstants.SECKILL_DEDUPE, userId, batchId);
        Boolean setSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupeKey, "1", RedisKeyConstants.SECKILL_DEDUPE_TTL, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(setSuccess)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已参与过本轮抢购");
        }

        // 7. Lua 原子扣库存
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        String versionStr = (String) stringRedisTemplate.opsForHash().get(stockKey, "version");
        long version = versionStr != null ? Long.parseLong(versionStr) : 0;

        Long result = stringRedisTemplate.execute(
                seckillDeductScript,
                Collections.singletonList(stockKey),
                "1", String.valueOf(version)
        );

        if (result == null || result == -1) {
            // 库存不足，清理防重标记
            stringRedisTemplate.delete(dedupeKey);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "编码券已抢光");
        }
        if (result == -2) {
            // 版本冲突，清理防重标记，让用户重试
            stringRedisTemplate.delete(dedupeKey);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请重试");
        }

        // 8. 生成订单号
        String orderNo = generateOrderNo();

        // 9. 发 MQ 消息（异步写 DB）
        SeckillMessage msg = new SeckillMessage(userId, batchId, orderNo);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_EXCHANGE,
                    RabbitMQConfig.SECKILL_ROUTING_KEY,
                    JSONUtil.toJsonStr(msg)
            );
        } catch (Exception e) {
            // MQ 发送失败，回滚库存
            rollbackStock(batchId);
            stringRedisTemplate.delete(dedupeKey);
            log.error("秒杀消息发送失败, userId={}, batchId={}", userId, batchId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
        }

        log.info("秒杀抢购排队中, userId={}, batchId={}, orderNo={}", userId, batchId, orderNo);
        return orderNo;
    }

    @Override
    public SeckillOrder getResult(Long userId, String orderNo) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getOrderNo, orderNo)
               .eq(SeckillOrder::getUserId, userId);
        SeckillOrder order = this.getOne(wrapper);
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单不存在");
        }
        return order;
    }

    @Override
    public void preheat(Long batchId) {
        CodeCouponBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "批次不存在");
        }

        // 1. 写入 Redis 库存 Hash
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        Map<String, String> stockMap = new HashMap<>();
        stockMap.put("stock", String.valueOf(batch.getTotalStock()));
        stockMap.put("version", "0");
        stringRedisTemplate.opsForHash().putAll(stockKey, stockMap);

        // 2. 写入 Redis 批次详情
        String infoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        stringRedisTemplate.opsForValue().set(infoKey, JSONUtil.toJsonStr(batch));

        // 3. 更新批次状态为"进行中"（简化：预热完直接进行中）
        batch.setStatus(2);
        batchMapper.updateById(batch);

        log.info("批次预热完成, batchId={}, stock={}", batchId, batch.getTotalStock());
    }

    /**
     * 获取批次信息（前端倒计时用）
     */
    public Map<String, Object> getBatchInfo(Long batchId) {
        String infoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String batchInfoJson = stringRedisTemplate.opsForValue().get(infoKey);

        if (StrUtil.isNotBlank(batchInfoJson)) {
            return JSONUtil.toBean(batchInfoJson, Map.class);
        }

        // Redis 未命中，查 DB
        CodeCouponBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "批次不存在");
        }
        return JSONUtil.toBean(JSONUtil.toJsonStr(batch), Map.class);
    }

    // ---- 内部方法 ----

    private void checkBatchExists(Long batchId) {
        String infoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String json = stringRedisTemplate.opsForValue().get(infoKey);
        if (StrUtil.isBlank(json)) {
            CodeCouponBatch batch = batchMapper.selectById(batchId);
            if (batch == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "批次不存在");
            }
        }
    }

    private void checkBatchTime(Map<String, Object> batchInfo) {
        // 简化：检查 status 字段
        Object status = batchInfo.get("status");
        if (status != null) {
            int s = Integer.parseInt(status.toString());
            if (s == 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动未开始");
            }
            if (s == 3) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动已结束");
            }
            if (s == 4) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "活动已取消");
            }
        }
    }

    private void rollbackStock(Long batchId) {
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        stringRedisTemplate.opsForHash().increment(stockKey, "stock", 1);
        stringRedisTemplate.opsForHash().increment(stockKey, "version", 1);
    }

    private String generateOrderNo() {
        return "SK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));
    }
}
```

### 3.1 幂等性保障说明（HMAC 改造后）

HMAC Token 本身不提供一次性保证（token 可被重放），幂等性由以下三层共同保证：

| 层级 | 机制 | 防护范围 |
|------|------|---------|
| L1 | HMAC 签名 + timestamp 过期 | 防篡改、防过期 token 重用 |
| L2 | Redis dedupe SET NX（userId+batchId） | 防同一用户同一批次重复抢购 |
| L3 | MQ 消费者 DB 查重（selectCount） | 防消息重复投递导致重复写单 |

L2 是核心幂等保障：HMAC token 中编码了 userId+batchId，dedupe key 也是 userId+batchId，同一 token 重放必然命中同一把 dedupe 锁。

---

## 4. MQ 消费者

创建 `server/src/main/java/org/example/server/service/mq/SeckillConsumer.java`：

```java
package org.example.server.service.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Date;

/**
 * 秒杀消息消费者 — 异步写 DB
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

    @RabbitListener(queues = "seckill.queue")
    @Transactional(rollbackFor = Exception.class)
    public void handleSeckillGrab(String message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到秒杀消息: {}", message);
        SeckillMessage msg = null;

        try {
            msg = JSONUtil.toBean(message, SeckillMessage.class);

            // 1. 幂等检查（DB 防重表）
            LambdaQueryWrapper<SeckillOrder> existsWrapper = new LambdaQueryWrapper<>();
            existsWrapper.eq(SeckillOrder::getUserId, msg.getUserId())
                         .eq(SeckillOrder::getBatchId, msg.getBatchId());
            if (seckillOrderMapper.selectCount(existsWrapper) > 0) {
                log.warn("重复消费，跳过: userId={}, batchId={}", msg.getUserId(), msg.getBatchId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 写秒杀订单
            SeckillOrder order = new SeckillOrder();
            order.setUserId(msg.getUserId());
            order.setBatchId(msg.getBatchId());
            order.setOrderNo(msg.getOrderNo());
            order.setStatus(0); // 处理中
            seckillOrderMapper.insert(order);

            // 3. 分配编码券（FOR UPDATE SKIP LOCKED 非阻塞锁）
            CodeCoupon coupon = codeCouponMapper.selectOne(
                    new LambdaQueryWrapper<CodeCoupon>()
                            .eq(CodeCoupon::getBatchId, msg.getBatchId())
                            .eq(CodeCoupon::getStatus, 0)
                            .last("FOR UPDATE SKIP LOCKED")
                            .last("LIMIT 1")
            );

            if (coupon == null) {
                // 极端情况：DB 无可用券，回滚 Redis 库存
                log.error("DB 无可用券! userId={}, batchId={}", msg.getUserId(), msg.getBatchId());
                rollbackRedisStock(msg.getBatchId());
                order.setStatus(2); // 失败
                seckillOrderMapper.updateById(order);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 4. 绑定用户
            coupon.setUserId(msg.getUserId());
            coupon.setStatus(1); // 已领取
            coupon.setIssuedAt(new Date());
            coupon.setExpireAt(new Date(System.currentTimeMillis()
                    + (long) coupon.getType() * 24 * 3600 * 1000));
            codeCouponMapper.updateById(coupon);

            // 5. 回填券 ID 到订单
            order.setCouponId(coupon.getId());
            order.setStatus(1); // 成功
            seckillOrderMapper.updateById(order);

            // 6. WebSocket 通知用户（通过 SSE 或后续集成 WebSocket）
            // TODO: 集成 WebSocket 推送

            log.info("秒杀处理成功, userId={}, batchId={}, couponId={}",
                    msg.getUserId(), msg.getBatchId(), coupon.getId());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("秒杀处理异常, message={}", message, e);
            try {
                // 拒绝消息，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("消息拒绝失败", ex);
            }
        }
    }

    /**
     * 回滚 Redis 库存
     */
    private void rollbackRedisStock(Long batchId) {
        // 使用 Lua 脚本归还库存保证原子性
        // 简化实现：直接 HINCRBY
        String stockKey = String.format("seckill:batch:stock:%d", batchId);
        stringRedisTemplate.opsForHash().increment(stockKey, "stock", 1);
        stringRedisTemplate.opsForHash().increment(stockKey, "version", 1);
    }
}
```

---

## 5. 降级策略服务

创建 `server/src/main/java/org/example/server/service/SeckillDegradationService.java`：

```java
package org.example.server.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 秒杀降级策略服务
 *
 * 降级等级：
 * 0 - 正常（全部可用）
 * 1 - Redis限流降级（跳过Redis限流，走DB限流）
 * 2 - MQ降级（同步写DB，响应变慢）
 * 3 - 只读降级（暂停抢购，可查询结果）
 * 4 - 全部降级（显示活动已结束）
 */
@Slf4j
@Service
public class SeckillDegradationService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public int getDegradeLevel() {
        try {
            String level = stringRedisTemplate.opsForValue().get(RedisKeyConstants.SECKILL_DEGRADE);
            return level != null ? Integer.parseInt(level) : 0;
        } catch (Exception e) {
            log.warn("获取降级等级异常，默认正常", e);
            return 0;
        }
    }

    public boolean isSeckillEnabled() {
        return getDegradeLevel() < 3;
    }

    public boolean isMqEnabled() {
        return getDegradeLevel() < 2;
    }

    public boolean isRateLimitEnabled() {
        return getDegradeLevel() < 1;
    }

    /**
     * 设置降级等级（管理后台调用）
     */
    public void setDegradeLevel(int level) {
        stringRedisTemplate.opsForValue().set(RedisKeyConstants.SECKILL_DEGRADE, String.valueOf(level));
        log.warn("秒杀降级等级已设置为: {}", level);
    }
}
```

---

## 6. 定时任务

创建 `server/src/main/java/org/example/server/scheduled/SeckillScheduled.java`：

```java
package org.example.server.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.example.server.service.SeckillService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 秒杀相关定时任务
 */
@Slf4j
@Component
public class SeckillScheduled {

    @Resource
    private SeckillService seckillService;

    /**
     * 自动预热 — 每分钟检查是否有即将开始的批次
     * 活动开始前 15 分钟触发预热
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoPreheat() {
        // TODO: 查询 DB 中 start_time 在未来 15 分钟内的、status=0(草稿) 的批次
        // 对每个批次调用 seckillService.preheat(batchId)
        log.debug("自动预热检查...");
    }
}
```

**注意**：过期券回收、库存对账等定时任务将在 Phase 4 中实现（因为依赖 CouponService）。

---

## 验证清单

- [ ] `GET /api/seckill/token?batchId=1` 返回 HMAC 签名 Token（Base64URL 格式）
- [ ] Token 过期后（5 分钟）抢购被拒绝，提示"Token 已过期"
- [ ] 篡改 Token 内容后抢购被拒绝（签名校验失败）
- [ ] 使用他人 Token 抢购被拒绝（userId 不匹配）
- [ ] `POST /api/seckill/grab` 成功扣减 Redis 库存并发送 MQ 消息
- [ ] MQ 消费者正确写 seckill_order + code_coupon 两张表
- [ ] `GET /api/seckill/result?orderNo=xxx` 返回正确状态
- [ ] 重复抢购（同一用户同一批次）被 dedupe 拦截
- [ ] IP 限流：60 秒内超过 10 次请求被拒绝
- [ ] 用户限流：3600 秒内超过 3 次请求被拒绝
- [ ] 库存为 0 时抢购返回"编码券已抢光"
- [ ] MQ 发送失败时 Redis 库存正确回滚
- [ ] 降级开关设置为 3 时抢购被拒绝
- [ ] 死信消息被正确消费并记录日志
