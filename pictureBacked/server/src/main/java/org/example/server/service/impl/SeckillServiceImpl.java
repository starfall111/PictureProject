package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.RateLimitUtil;
import org.example.pojo.dto.seckill.BatchQueryDTO;
import org.example.pojo.dto.seckill.SeckillMessage;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.pojo.entity.SeckillOrder;
import org.example.pojo.vo.seckill.PublicBatchVO;
import org.example.server.config.RabbitMQConfig;
import org.example.server.mapper.CodeCouponBatchMapper;
import org.example.server.mapper.SeckillOrderMapper;
import org.example.server.service.SeckillDegradationService;
import org.example.server.service.SeckillService;
import org.example.server.util.SeckillTokenUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务核心实现
 * <p>
 * 流程: Token 签发 -> 抢购(降级检查 + 验签 + 活动校验 + 限流 + 防重 + Lua 原子扣库存 + MQ 异步下单)
 * </p>
 *
 * @author Zou
 */
@Slf4j
@Service
public class SeckillServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements SeckillService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CodeCouponBatchMapper codeCouponBatchMapper;

    @Resource
    private SeckillTokenUtil seckillTokenUtil;

    @Resource
    private RateLimitUtil rateLimitUtil;

    @Resource
    private SeckillDegradationService degradationService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource(name = "seckillDeductScript")
    private DefaultRedisScript<Long> seckillDeductScript;

    @Resource(name = "seckillRollbackScript")
    private DefaultRedisScript<Long> seckillRollbackScript;

    /** 秒杀限流：每个 IP 每秒最多 5 次 */
    private static final int RATE_LIMIT_IP_WINDOW = 1;
    private static final int RATE_LIMIT_IP_MAX = 5;

    /** 秒杀限流：每个用户每 10 秒最多 3 次 */
    private static final int RATE_LIMIT_USER_WINDOW = 10;
    private static final int RATE_LIMIT_USER_MAX = 3;

    // ==================== 获取令牌 ====================

    @Override
    public String getToken(Long userId, Long batchId) {
        ThrowUtils.throwIf(userId == null || batchId == null, ErrorCode.PARAMS_ERROR);

        // 检查批次是否存在
        CodeCouponBatch batch = codeCouponBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");

        // 生成 HMAC 令牌
        String token = seckillTokenUtil.generate(userId, batchId);
        log.info("秒杀令牌已签发 | userId={}, batchId={}", userId, batchId);
        return token;
    }

    // ==================== 秒杀抢券 ====================

    @Override
    public String grab(Long userId, Long batchId, String token, String clientIP) {
        ThrowUtils.throwIf(userId == null || batchId == null || StrUtil.isBlank(token),
                ErrorCode.PARAMS_ERROR, "参数不完整");

        // 1. 降级检查
        int degradeLevel = degradationService.getDegradationLevel();
        ThrowUtils.throwIf(degradeLevel >= 4, ErrorCode.OPERATION_ERROR, "秒杀活动暂不可用，请稍后再试");
        ThrowUtils.throwIf(degradeLevel >= 3, ErrorCode.OPERATION_ERROR, "秒杀活动已暂停，请稍后再试");

        // 2. HMAC Token 验签
        String[] fields = seckillTokenUtil.verify(token);
        Long tokenUserId = Long.parseLong(fields[0]);
        Long tokenBatchId = Long.parseLong(fields[1]);
        ThrowUtils.throwIf(!userId.equals(tokenUserId) || !batchId.equals(tokenBatchId),
                ErrorCode.PARAMS_ERROR, "令牌与请求不匹配");

        // 3. 活动状态校验 — 查 Redis 缓存，未命中查 DB
        String batchInfoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String batchInfoJson = stringRedisTemplate.opsForValue().get(batchInfoKey);
        CodeCouponBatch batch;
        if (StrUtil.isNotBlank(batchInfoJson)) {
            batch = JSONUtil.toBean(batchInfoJson, CodeCouponBatch.class);
        } else {
            batch = codeCouponBatchMapper.selectById(batchId);
            ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }

        // 活动时间校验
        Date now = new Date();
        ThrowUtils.throwIf(now.before(batch.getStartTime()), ErrorCode.OPERATION_ERROR, "活动尚未开始");
        ThrowUtils.throwIf(now.after(batch.getEndTime()), ErrorCode.OPERATION_ERROR, "活动已结束");
        // 活动状态: 2-进行中
        ThrowUtils.throwIf(!Integer.valueOf(2).equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "活动状态异常");

        // 4. IP 限流
        if (degradeLevel < 1) {
            RateLimitUtil.Result ipResult = rateLimitUtil.checkRateLimit(
                    "seckill:ip", clientIP, RATE_LIMIT_IP_WINDOW, RATE_LIMIT_IP_MAX);
            ThrowUtils.throwIf(!ipResult.allowed(), ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        // 5. 用户限流
        if (degradeLevel < 1) {
            RateLimitUtil.Result userResult = rateLimitUtil.checkRateLimit(
                    "seckill:user", String.valueOf(userId), RATE_LIMIT_USER_WINDOW, RATE_LIMIT_USER_MAX);
            ThrowUtils.throwIf(!userResult.allowed(), ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        // 6. 防重复下单（Redis SET NX）
        String dedupeKey = String.format(RedisKeyConstants.SECKILL_DEDUPE, userId, batchId);
        Boolean dedupeSet = stringRedisTemplate.opsForValue().setIfAbsent(
                dedupeKey, "1", RedisKeyConstants.SECKILL_DEDUPE_TTL, TimeUnit.SECONDS);
        ThrowUtils.throwIf(!Boolean.TRUE.equals(dedupeSet),
                ErrorCode.OPERATION_ERROR, "请勿重复参与");

        // 7. Lua 原子扣库存
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        String versionStr = (String) stringRedisTemplate.opsForHash().get(stockKey, "version");
        long version = versionStr != null ? Long.parseLong(versionStr) : 0L;

        Long deductResult;
        try {
            deductResult = stringRedisTemplate.execute(
                    seckillDeductScript,
                    List.of(stockKey),
                    "1", String.valueOf(version)
            );
        } catch (Exception e) {
            log.error("Lua 扣库存异常，回滚去重标记 | userId={}, batchId={}", userId, batchId, e);
            rollbackDedupe(dedupeKey);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        }

        if (deductResult == null || deductResult != 1L) {
            // 扣库存失败，回滚去重标记
            rollbackDedupe(dedupeKey);
            if (deductResult != null && deductResult == -1L) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已售罄");
            }
            if (deductResult != null && deductResult == -2L) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请重试");
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        }

        // 8. 生成订单号
        String orderNo = generateOrderNo(userId, batchId);

        // 9. 发 MQ（降级等级 >= 2 时不发 MQ）
        if (degradeLevel < 2) {
            try {
                SeckillMessage message = new SeckillMessage();
                message.setUserId(userId);
                message.setBatchId(batchId);
                message.setOrderNo(orderNo);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.SECKILL_EXCHANGE,
                        RabbitMQConfig.SECKILL_ROUTING_KEY,
                        JSONUtil.toJsonStr(message)
                );
                log.info("秒杀订单 MQ 已发送 | userId={}, batchId={}, orderNo={}", userId, batchId, orderNo);
            } catch (Exception e) {
                log.error("MQ 发送失败，回滚库存 | userId={}, batchId={}, orderNo={}", userId, batchId, orderNo, e);
                rollbackStock(stockKey);
                rollbackDedupe(dedupeKey);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
            }
        } else {
            // MQ 降级：直接写 DB
            log.warn("MQ 降级模式，同步写订单 | userId={}, batchId={}", userId, batchId);
            SeckillOrder order = new SeckillOrder();
            order.setUserId(userId);
            order.setBatchId(batchId);
            order.setOrderNo(orderNo);
            order.setStatus(0);
            this.save(order);
        }

        return orderNo;
    }

    // ==================== 查询结果 ====================

    @Override
    public SeckillOrder getResult(Long userId, String orderNo) {
        ThrowUtils.throwIf(userId == null || StrUtil.isBlank(orderNo), ErrorCode.PARAMS_ERROR);

        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getOrderNo, orderNo)
                .eq(SeckillOrder::getUserId, userId);
        SeckillOrder order = this.getOne(wrapper);
        ThrowUtils.throwIf(order == null, ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        return order;
    }

    // ==================== 预热库存 ====================

    @Override
    public void preheat(Long batchId) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);

        CodeCouponBatch batch = codeCouponBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");

        // 写 Redis 库存 Hash
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        Map<String, String> stockFields = new HashMap<>();
        stockFields.put("stock", String.valueOf(batch.getCurrentStock()));
        stockFields.put("version", "0");
        stringRedisTemplate.opsForHash().putAll(stockKey, stockFields);

        // 设置过期时间：活动结束时间 + 1 小时
        long expireSeconds = (batch.getEndTime().getTime() - System.currentTimeMillis()) / 1000 + 3600;
        if (expireSeconds > 0) {
            stringRedisTemplate.expire(stockKey, expireSeconds, TimeUnit.SECONDS);
        }

        // 写 Redis 批次详情缓存
        String batchInfoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String batchJson = JSONUtil.toJsonStr(batch);
        if (expireSeconds > 0) {
            stringRedisTemplate.opsForValue().set(batchInfoKey, batchJson, expireSeconds, TimeUnit.SECONDS);
        } else {
            stringRedisTemplate.opsForValue().set(batchInfoKey, batchJson);
        }

        // 更新批次状态为预热中
        batch.setStatus(1);
        codeCouponBatchMapper.updateById(batch);

        log.info("秒杀批次预热完成 | batchId={}, stock={}", batchId, batch.getCurrentStock());
    }

    // ==================== 获取批次信息 ====================

    @Override
    public Map<String, Object> getBatchInfo(Long batchId) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);

        // 先查 Redis 批次详情缓存
        String batchInfoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String batchInfoJson = stringRedisTemplate.opsForValue().get(batchInfoKey);

        CodeCouponBatch batch;
        if (StrUtil.isNotBlank(batchInfoJson)) {
            batch = JSONUtil.toBean(batchInfoJson, CodeCouponBatch.class);
        } else {
            // 未命中，查 DB
            batch = codeCouponBatchMapper.selectById(batchId);
            ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }

        // 查 Redis 库存
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        String stockStr = (String) stringRedisTemplate.opsForHash().get(stockKey, "stock");
        int remainStock = stockStr != null ? Integer.parseInt(stockStr) : batch.getCurrentStock();

        Map<String, Object> result = new HashMap<>();
        result.put("id", batch.getId());
        result.put("name", batch.getName());
        result.put("type", batch.getType());
        result.put("totalStock", batch.getTotalStock());
        result.put("remainStock", remainStock);
        result.put("startTime", batch.getStartTime());
        result.put("endTime", batch.getEndTime());
        result.put("status", batch.getStatus());
        return result;
    }

    // ==================== 获取批次列表（公开） ====================

    // 应该去 redis 中查，避免直接查 DB
    @Override
    public Page<PublicBatchVO> listBatches(BatchQueryDTO dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);

        int current = Math.max(dto.getCurrent(), 1);
        int pageSize = Math.min(Math.max(dto.getPageSize(), 1), 50);

        LambdaQueryWrapper<CodeCouponBatch> wrapper = new LambdaQueryWrapper<>();

        // 只返回用户可见的批次：预热中(1)、进行中(2)、已结束(3)
        Integer status = dto.getStatus();
        if (status != null) {
            // 限制只允许查询可见状态
            ThrowUtils.throwIf(status < 1 || status > 3,
                    ErrorCode.PARAMS_ERROR, "状态筛选值无效，只允许 1(预热中)/2(进行中)/3(已结束)");
            wrapper.eq(CodeCouponBatch::getStatus, status);
        } else {
            wrapper.in(CodeCouponBatch::getStatus, 1, 2, 3);
        }

        // 优先展示进行中的批次，其次预热中，最后已结束；同状态按开始时间倒序
        wrapper.orderByAsc(CodeCouponBatch::getStatus)
                .orderByDesc(CodeCouponBatch::getStartTime);

        Page<CodeCouponBatch> batchPage = codeCouponBatchMapper.selectPage(
                new Page<>(current, pageSize), wrapper);

        // 转换为 PublicBatchVO
        Page<PublicBatchVO> voPage = new Page<>(batchPage.getCurrent(), batchPage.getSize(), batchPage.getTotal());
        List<PublicBatchVO> voList = batchPage.getRecords().stream().map(batch -> {
            PublicBatchVO vo = new PublicBatchVO();
            vo.setId(batch.getId());
            vo.setName(batch.getName());
            vo.setType(batch.getType());
            vo.setTotalStock(batch.getTotalStock());
            vo.setRemainStock(batch.getCurrentStock());
            vo.setStartTime(batch.getStartTime());
            vo.setEndTime(batch.getEndTime());
            vo.setStatus(batch.getStatus());
            // 计算售出百分比
            if (batch.getTotalStock() != null && batch.getTotalStock() > 0) {
                int sold = batch.getTotalStock() - (batch.getCurrentStock() != null ? batch.getCurrentStock() : 0);
                vo.setProgress((int) ((long) sold * 100 / batch.getTotalStock()));
            } else {
                vo.setProgress(0);
            }
            return vo;
        }).toList();
        voPage.setRecords(voList);

        return voPage;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成订单号: 时间戳 + userId 后6位 + batchId 后4位 + 4位随机数
     */
    private String generateOrderNo(Long userId, Long batchId) {
        String ts = String.valueOf(System.currentTimeMillis());
        String uidSuffix = String.format("%06d", userId % 1000000);
        String bidSuffix = String.format("%04d", batchId % 10000);
        String random = String.format("%04d", new Random().nextInt(10000));
        return ts + uidSuffix + bidSuffix + random;
    }

    /**
     * 回滚去重标记
     */
    private void rollbackDedupe(String dedupeKey) {
        try {
            stringRedisTemplate.delete(dedupeKey);
        } catch (Exception ex) {
            log.error("回滚去重标记失败 | key={}", dedupeKey, ex);
        }
    }

    /**
     * 回滚 Redis 库存（Lua 脚本 +1）
     */
    private void rollbackStock(String stockKey) {
        try {
            stringRedisTemplate.execute(seckillRollbackScript, List.of(stockKey), "1");
        } catch (Exception ex) {
            log.error("回滚库存失败 | key={}", stockKey, ex);
        }
    }
}
