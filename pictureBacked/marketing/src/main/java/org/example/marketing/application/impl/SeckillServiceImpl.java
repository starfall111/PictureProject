package org.example.marketing.application.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.util.RateLimitUtil;
import org.example.marketing.interfaces.dto.BatchQueryDTO;
import org.example.marketing.interfaces.dto.SeckillMessage;
import org.example.marketing.domain.model.CodeCouponBatch;
import org.example.marketing.domain.model.SeckillOrder;
import org.example.marketing.interfaces.vo.PublicBatchVO;
import org.example.marketing.infrastructure.mq.SeckillMQConfig;
import org.example.marketing.infrastructure.persistence.CodeCouponBatchMapper;
import org.example.marketing.infrastructure.persistence.SeckillOrderMapper;
import org.example.marketing.application.SeckillDegradationService;
import org.example.marketing.application.SeckillService;
import org.example.marketing.application.util.SeckillTokenUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
                        SeckillMQConfig.SECKILL_EXCHANGE,
                        SeckillMQConfig.SECKILL_ROUTING_KEY,
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

        // 加入等待开始集合
        String setKey = String.format(RedisKeyConstants.SECKILL_BATCH_STATUS_SET, 1);
        stringRedisTemplate.opsForSet().add(setKey, String.valueOf(batchId));

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

    /**
     * 获取批次列表（公开）— 状态集合 + 实时拼装模式
     * <p>
     * 查询流程：读 Redis 状态 Set → 缺失则加锁从 DB 回填 → Pipeline 读 info+stock → 拼装 VO → 内存排序分页
     * </p>
     */
    @Override
    public Page<PublicBatchVO> listBatches(BatchQueryDTO dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);

        int current = Math.max(dto.getCurrent(), 1);
        int pageSize = Math.min(Math.max(dto.getPageSize(), 1), 50);

        Integer status = dto.getStatus();
        if (status != null) {
            ThrowUtils.throwIf(status < 1 || status > 3,
                    ErrorCode.PARAMS_ERROR, "状态筛选值无效，只允许 1(等待开始)/2(进行中)/3(已结束)");
        }

        // 1. 从 Redis 状态 Set 获取 batchId 列表（缺失则从 DB 回填）
        List<String> batchIdStrs = getBatchIdsByStatus(status);
        if (batchIdStrs.isEmpty()) {
            return new Page<>(current, pageSize, 0);
        }

        // 2. 逐个读取 info + stock，拼装 VO
        List<PublicBatchVO> allVOs = new ArrayList<>();
        for (String idStr : batchIdStrs) {
            try {
                PublicBatchVO vo = buildBatchVOFromRedis(Long.parseLong(idStr));
                if (vo != null) {
                    allVOs.add(vo);
                }
            } catch (NumberFormatException e) {
                log.warn("Set 中存在非法 batchId: {}", idStr);
            }
        }

        // 3. 内存排序：进行中(2) > 等待开始(1) > 已结束(3)，同状态按开始时间倒序
        allVOs.sort((a, b) -> {
            int orderA = statusOrder(a.getStatus());
            int orderB = statusOrder(b.getStatus());
            if (orderA != orderB) {
                return Integer.compare(orderA, orderB);
            }
            if (a.getStartTime() != null && b.getStartTime() != null) {
                return b.getStartTime().compareTo(a.getStartTime());
            }
            return 0;
        });

        // 4. 内存分页
        long total = allVOs.size();
        int fromIndex = Math.min((current - 1) * pageSize, allVOs.size());
        int toIndex = Math.min(fromIndex + pageSize, allVOs.size());
        List<PublicBatchVO> pageRecords = allVOs.subList(fromIndex, toIndex);

        Page<PublicBatchVO> result = new Page<>(current, pageSize, total);
        result.setRecords(pageRecords);
        return result;
    }

    /**
     * 状态排序权重：进行中(2)=1 最前，等待开始(1)=2，已结束(3)=3
     */
    private int statusOrder(Integer status) {
        if (status == null) return 99;
        return switch (status) {
            case 2 -> 1;
            case 1 -> 2;
            case 3 -> 3;
            default -> 99;
        };
    }

    /**
     * 从 Redis 状态 Set 获取 batchId 列表
     * <p>
     * Set 不存在（冷启动/Redis 重启）时，加锁从 DB 回填所有 Set
     * </p>
     */
    private List<String> getBatchIdsByStatus(Integer status) {
        if (status != null) {
            String setKey = String.format(RedisKeyConstants.SECKILL_BATCH_STATUS_SET, status);
            Set<String> members = stringRedisTemplate.opsForSet().members(setKey);
            if (members != null && !members.isEmpty()) {
                return new ArrayList<>(members);
            }
            // Set 为空或不存在，检查是否需要从 DB 回填
            if (!isAnyStatusSetExists()) {
                rebuildStatusSetsFromDB();
                members = stringRedisTemplate.opsForSet().members(setKey);
                return members != null ? new ArrayList<>(members) : Collections.emptyList();
            }
            // Set 已初始化但确实没有该状态的数据
            return Collections.emptyList();
        }

        // 无状态筛选：合并三个 Set
        Set<String> allMembers = new LinkedHashSet<>();
        for (int s = 1; s <= 3; s++) {
            String setKey = String.format(RedisKeyConstants.SECKILL_BATCH_STATUS_SET, s);
            Set<String> members = stringRedisTemplate.opsForSet().members(setKey);
            if (members != null) {
                allMembers.addAll(members);
            }
        }

        if (!allMembers.isEmpty()) {
            return new ArrayList<>(allMembers);
        }

        // 冷启动：从 DB 回填
        if (!isAnyStatusSetExists()) {
            rebuildStatusSetsFromDB();
            for (int s = 1; s <= 3; s++) {
                String setKey = String.format(RedisKeyConstants.SECKILL_BATCH_STATUS_SET, s);
                Set<String> members = stringRedisTemplate.opsForSet().members(setKey);
                if (members != null) {
                    allMembers.addAll(members);
                }
            }
        }
        return new ArrayList<>(allMembers);
    }

    /**
     * 检查是否至少有一个状态集合已存在于 Redis（用于区分冷启动 vs 合法的空集合）
     */
    private boolean isAnyStatusSetExists() {
        for (int s = 1; s <= 3; s++) {
            String setKey = String.format(RedisKeyConstants.SECKILL_BATCH_STATUS_SET, s);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(setKey))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 DB 重建状态集合（冷启动/Redis 重启后回填）
     * <p>
     * 加分布式锁防并发回填，回填内容包括：3 个状态 Set + 缺失的 info 缓存
     * </p>
     */
    private void rebuildStatusSetsFromDB() {
        String lockKey = RedisKeyConstants.LOCK_SECKILL_SET_REBUILD;
        if (!tryLock(lockKey, RedisKeyConstants.SECKILL_SET_REBUILD_LOCK_TTL)) {
            // 未获取到锁，短暂等待后返回（其他线程正在回填）
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        try {
            // Double-check：再查一次是否有 Set 已存在
            if (isAnyStatusSetExists()) {
                return;
            }

            LambdaQueryWrapper<CodeCouponBatch> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(CodeCouponBatch::getStatus, 1, 2, 3);
            List<CodeCouponBatch> batches = codeCouponBatchMapper.selectList(wrapper);
            if (batches.isEmpty()) {
                // 即使没有数据，也创建空集合标记已初始化
                for (int s = 1; s <= 3; s++) {
                    String setKey = String.format(RedisKeyConstants.SECKILL_BATCH_STATUS_SET, s);
                    stringRedisTemplate.opsForSet().add(setKey, "__init__");
                    stringRedisTemplate.opsForSet().remove(setKey, "__init__");
                }
                return;
            }

            // 按状态分组写入 Set
            Map<Integer, List<CodeCouponBatch>> grouped = batches.stream()
                    .collect(Collectors.groupingBy(CodeCouponBatch::getStatus));

            for (Map.Entry<Integer, List<CodeCouponBatch>> entry : grouped.entrySet()) {
                String setKey = String.format(RedisKeyConstants.SECKILL_BATCH_STATUS_SET, entry.getKey());
                String[] batchIds = entry.getValue().stream()
                        .map(b -> String.valueOf(b.getId()))
                        .toArray(String[]::new);
                stringRedisTemplate.opsForSet().add(setKey, batchIds);
            }

            // 补充缺失的状态 Set（空集合，标记已初始化）
            for (int s = 1; s <= 3; s++) {
                String setKey = String.format(RedisKeyConstants.SECKILL_BATCH_STATUS_SET, s);
                if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(setKey))) {
                    stringRedisTemplate.opsForSet().add(setKey, "__init__");
                    stringRedisTemplate.opsForSet().remove(setKey, "__init__");
                }
            }

            // 同时回填缺失的 info 缓存
            for (CodeCouponBatch batch : batches) {
                String batchInfoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batch.getId());
                if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(batchInfoKey))) {
                    long expireSeconds = (batch.getEndTime().getTime() - System.currentTimeMillis()) / 1000 + 3600;
                    String batchJson = JSONUtil.toJsonStr(batch);
                    if (expireSeconds > 0) {
                        stringRedisTemplate.opsForValue().set(batchInfoKey, batchJson, expireSeconds, TimeUnit.SECONDS);
                    } else {
                        stringRedisTemplate.opsForValue().set(batchInfoKey, batchJson);
                    }
                }
            }

            log.info("秒杀批次状态集合已从 DB 重建 | 总数={}", batches.size());
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 从 Redis 读取单个批次的 info + stock 拼装 PublicBatchVO
     * <p>
     * info 缺失时从 DB 加载并回填；stock 缺失则用 DB 中的 currentStock 兜底
     * </p>
     */
    private PublicBatchVO buildBatchVOFromRedis(Long batchId) {
        String batchInfoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);

        // 读 info
        String batchInfoJson = stringRedisTemplate.opsForValue().get(batchInfoKey);
        CodeCouponBatch batch;
        if (StrUtil.isNotBlank(batchInfoJson)) {
            batch = JSONUtil.toBean(batchInfoJson, CodeCouponBatch.class);
        } else {
            batch = codeCouponBatchMapper.selectById(batchId);
            if (batch == null) {
                log.warn("批次不存在，可能已被删除 | batchId={}", batchId);
                return null;
            }
            long expireSeconds = (batch.getEndTime().getTime() - System.currentTimeMillis()) / 1000 + 3600;
            String batchJson = JSONUtil.toJsonStr(batch);
            if (expireSeconds > 0) {
                stringRedisTemplate.opsForValue().set(batchInfoKey, batchJson, expireSeconds, TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.opsForValue().set(batchInfoKey, batchJson);
            }
        }

        // 读 stock（实时库存）
        String stockStr = (String) stringRedisTemplate.opsForHash().get(stockKey, "stock");
        int remainStock = stockStr != null ? Integer.parseInt(stockStr) : batch.getCurrentStock();

        // 拼装 VO
        PublicBatchVO vo = new PublicBatchVO();
        vo.setId(batch.getId());
        vo.setName(batch.getName());
        vo.setType(batch.getType());
        vo.setTotalStock(batch.getTotalStock());
        vo.setRemainStock(remainStock);
        vo.setStartTime(batch.getStartTime());
        vo.setEndTime(batch.getEndTime());
        vo.setStatus(batch.getStatus());

        if (batch.getTotalStock() != null && batch.getTotalStock() > 0) {
            int sold = batch.getTotalStock() - remainStock;
            vo.setProgress((int) ((long) sold * 100 / batch.getTotalStock()));
        } else {
            vo.setProgress(0);
        }
        return vo;
    }

    // ==================== 私有方法 ====================

    /**
     * 尝试获取分布式锁（SET NX + TTL）
     *
     * @param key        锁 key
     * @param ttlSeconds 锁持有时间（秒）
     * @return 是否获取成功
     */
    private boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 释放分布式锁
     *
     * @param key 锁 key
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

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
