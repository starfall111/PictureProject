package org.example.server.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.dto.seckill.BatchCreateDTO;
import org.example.pojo.dto.seckill.BatchUpdateDTO;
import org.example.pojo.entity.CodeCoupon;
import org.example.pojo.entity.CodeCouponBatch;
import org.example.server.mapper.CodeCouponBatchMapper;
import org.example.server.mapper.CodeCouponMapper;
import org.example.server.service.BatchManageService;
import org.example.server.service.SeckillService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 批次管理服务实现（管理端）
 *
 * @author Zou
 */
@Slf4j
@Service
public class BatchManageServiceImpl implements BatchManageService {

    @Resource
    private CodeCouponBatchMapper batchMapper;

    @Resource
    private CodeCouponMapper couponMapper;

    @Resource
    private SeckillService seckillService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 允许的券类型 */
    private static final Set<Integer> VALID_TYPES = Set.of(3, 7, 30);

    /** 兑换码批量插入大小 */
    private static final int BATCH_INSERT_SIZE = 500;

    // ==================== 创建批次 ====================

    @Override
    public Long createBatch(BatchCreateDTO dto) {
        validateCreateParams(dto);

        CodeCouponBatch batch = new CodeCouponBatch();
        batch.setName(dto.getName());
        batch.setType(dto.getType());
        batch.setTotalStock(dto.getTotalStock());
        batch.setCurrentStock(dto.getTotalStock());
        batch.setStartTime(dto.getStartTime());
        batch.setEndTime(dto.getEndTime());
        batch.setBatchNo(generateBatchNo());
        batch.setStatus(0); // 草稿

        batchMapper.insert(batch);
        log.info("批次创建成功 | batchId={}, batchNo={}, type={}, stock={}",
                batch.getId(), batch.getBatchNo(), dto.getType(), dto.getTotalStock());
        return batch.getId();
    }

    // ==================== 生成兑换码 ====================

    @Override
    public void generateCodes(Long batchId) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);
        CodeCouponBatch batch = batchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!Integer.valueOf(0).equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有草稿状态的批次才能生成兑换码");

        // 幂等检查：已生成且数量等于 totalStock 则跳过
        Long existCount = couponMapper.selectCount(
                new LambdaQueryWrapper<CodeCoupon>()
                        .eq(CodeCoupon::getBatchId, batchId)
                        .eq(CodeCoupon::getStatus, 0));
        if (existCount != null && existCount.equals(Long.valueOf(batch.getTotalStock()))) {
            log.info("兑换码已生成，跳过 | batchId={}, count={}", batchId, existCount);
            return;
        }
        ThrowUtils.throwIf(existCount != null && existCount > 0,
                ErrorCode.OPERATION_ERROR, "该批次已有部分兑换码（" + existCount + "张），请先清理后再生成");

        // 批量生成
        int totalStock = batch.getTotalStock();
        int type = batch.getType() != null ? batch.getType() : 0;
        List<CodeCoupon> buffer = new ArrayList<>(BATCH_INSERT_SIZE);

        for (int i = 0; i < totalStock; i++) {
            CodeCoupon coupon = new CodeCoupon();
            coupon.setBatchId(batchId);
            coupon.setType(type);
            coupon.setStatus(0); // 未发放
            coupon.setCode(generateCouponCode(type));
            coupon.setExpireAt(roundToHour(addDays(batch.getEndTime(), 7)));
            buffer.add(coupon);

            if (buffer.size() >= BATCH_INSERT_SIZE) {
                batchInsertCoupons(buffer);
                buffer.clear();
            }
        }
        // 插入剩余的
        if (!buffer.isEmpty()) {
            batchInsertCoupons(buffer);
        }

        log.info("兑换码生成完成 | batchId={}, count={}", batchId, totalStock);
    }

    // ==================== 修改批次 ====================

    @Override
    public void updateBatch(BatchUpdateDTO dto) {
        ThrowUtils.throwIf(dto == null || dto.getId() == null, ErrorCode.PARAMS_ERROR);

        CodeCouponBatch batch = batchMapper.selectById(dto.getId());
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!Integer.valueOf(0).equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有草稿状态的批次才能修改");

        LambdaUpdateWrapper<CodeCouponBatch> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CodeCouponBatch::getId, dto.getId());
        wrapper.set(CodeCouponBatch::getEditTime, new Date());

        // 已领取券数量（用于库存变更校验）
        long claimedCount = couponMapper.selectCount(
                new LambdaQueryWrapper<CodeCoupon>()
                        .eq(CodeCoupon::getBatchId, dto.getId())
                        .gt(CodeCoupon::getStatus, 0));

        if (dto.getName() != null) {
            ThrowUtils.throwIf(StrUtil.isBlank(dto.getName()), ErrorCode.PARAMS_ERROR, "批次名称不能为空");
            wrapper.set(CodeCouponBatch::getName, dto.getName());
        }

        if (dto.getType() != null) {
            ThrowUtils.throwIf(!VALID_TYPES.contains(dto.getType()),
                    ErrorCode.PARAMS_ERROR, "券类型只能是 3/7/30");
            wrapper.set(CodeCouponBatch::getType, dto.getType());
            // 同步更新未发放券的 type
            couponMapper.update(null, new LambdaUpdateWrapper<CodeCoupon>()
                    .eq(CodeCoupon::getBatchId, dto.getId())
                    .eq(CodeCoupon::getStatus, 0)
                    .set(CodeCoupon::getType, dto.getType()));
        }

        if (dto.getTotalStock() != null) {
            ThrowUtils.throwIf(dto.getTotalStock() <= 0, ErrorCode.PARAMS_ERROR, "库存必须大于0");
            int newStock = dto.getTotalStock();
            int oldStock = batch.getTotalStock();
            ThrowUtils.throwIf(newStock < claimedCount,
                    ErrorCode.OPERATION_ERROR, "新库存不能小于已领取的券数量(" + claimedCount + ")");

            int diff = newStock - oldStock;
            wrapper.set(CodeCouponBatch::getTotalStock, newStock);
            wrapper.set(CodeCouponBatch::getCurrentStock, batch.getCurrentStock() + diff);

            if (diff > 0) {
                // 增加库存：补充生成兑换码
                generateExtraCodes(dto.getId(), batch.getType() != null ? batch.getType() : 0, diff, batch.getEndTime());
            } else if (diff < 0) {
                // 减少库存：删除多余的未发放券
                deleteExtraUnsoldCoupons(dto.getId(), -diff);
            }
        }

        if (dto.getStartTime() != null) {
            ThrowUtils.throwIf(dto.getStartTime().before(new Date()),
                    ErrorCode.PARAMS_ERROR, "开始时间不能早于当前时间");
            wrapper.set(CodeCouponBatch::getStartTime, dto.getStartTime());
        }

        if (dto.getEndTime() != null) {
            Date startTime = dto.getStartTime() != null ? dto.getStartTime() : batch.getStartTime();
            ThrowUtils.throwIf(!dto.getEndTime().after(startTime),
                    ErrorCode.PARAMS_ERROR, "结束时间必须晚于开始时间");
            wrapper.set(CodeCouponBatch::getEndTime, dto.getEndTime());
        }

        batchMapper.update(null, wrapper);
        log.info("批次修改成功 | batchId={}", dto.getId());
    }

    // ==================== 状态流转 ====================

    @Override
    public void transitionBatch(Long batchId, int targetStatus) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);

        String lockKey = String.format(RedisKeyConstants.LOCK_SECKILL_BATCH, batchId);
        boolean locked = tryLock(lockKey, 10);
        ThrowUtils.throwIf(!locked, ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");

        try {
            CodeCouponBatch batch = batchMapper.selectById(batchId);
            ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");

            int currentStatus = batch.getStatus();
            validateTransition(currentStatus, targetStatus, batch);

            // 草稿->预热中：先调用 preheat 写 Redis
            if (targetStatus == 1) {
                seckillService.preheat(batchId);
                // preheat 内部已更新状态为 1，无需再更新
                log.info("批次状态流转成功 | batchId={}, {}->{}", batchId, currentStatus, targetStatus);
                return;
            }

            // 更新 DB 状态
            int rows = batchMapper.update(null, new LambdaUpdateWrapper<CodeCouponBatch>()
                    .eq(CodeCouponBatch::getId, batchId)
                    .eq(CodeCouponBatch::getVersion, batch.getVersion())
                    .set(CodeCouponBatch::getStatus, targetStatus));
            ThrowUtils.throwIf(rows == 0, ErrorCode.OPERATION_ERROR, "状态更新失败，请重试");

            // 更新 Redis 缓存
            updateBatchInfoCache(batchId, targetStatus);

            log.info("批次状态流转成功 | batchId={}, {}->{}", batchId, currentStatus, targetStatus);
        } finally {
            unlock(lockKey);
        }
    }

    // ==================== 取消批次 ====================

    @Override
    public void cancelBatch(Long batchId) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);

        CodeCouponBatch batch = batchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!Integer.valueOf(0).equals(batch.getStatus())
                        && !Integer.valueOf(1).equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有草稿或预热中状态的批次才能取消");

        // 预热中取消：清理 Redis
        if (Integer.valueOf(1).equals(batch.getStatus())) {
            cleanBatchRedisCache(batchId);
        }

        // 逻辑删除所有未发放券
        couponMapper.update(null, new LambdaUpdateWrapper<CodeCoupon>()
                .eq(CodeCoupon::getBatchId, batchId)
                .eq(CodeCoupon::getStatus, 0)
                .set(CodeCoupon::getIsDelete, 1));

        // 更新批次状态
        batchMapper.update(null, new LambdaUpdateWrapper<CodeCouponBatch>()
                .eq(CodeCouponBatch::getId, batchId)
                .eq(CodeCouponBatch::getVersion, batch.getVersion())
                .set(CodeCouponBatch::getStatus, 4));

        log.info("批次取消成功 | batchId={}", batchId);
    }

    // ==================== 恢复批次 ====================

    @Override
    public void restoreBatch(Long batchId) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);

        CodeCouponBatch batch = batchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!Integer.valueOf(4).equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有已取消的批次才能恢复");

        // 清理 Redis 残留缓存
        cleanBatchRedisCache(batchId);

        // 恢复为草稿，重置库存
        batchMapper.update(null, new LambdaUpdateWrapper<CodeCouponBatch>()
                .eq(CodeCouponBatch::getId, batchId)
                .set(CodeCouponBatch::getStatus, 0)
                .set(CodeCouponBatch::getCurrentStock, batch.getTotalStock()));

        log.info("批次恢复为草稿 | batchId={}", batchId);
    }

    // ==================== 结束批次 ====================

    @Override
    public void endBatch(Long batchId) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);

        String lockKey = String.format(RedisKeyConstants.LOCK_SECKILL_BATCH, batchId);
        boolean locked = tryLock(lockKey, 10);
        ThrowUtils.throwIf(!locked, ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");

        try {
            CodeCouponBatch batch = batchMapper.selectById(batchId);
            ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
            ThrowUtils.throwIf(!Integer.valueOf(2).equals(batch.getStatus()),
                    ErrorCode.OPERATION_ERROR, "只有进行中的批次才能结束");

            // 先更新 DB 状态，阻止新请求
            int rows = batchMapper.update(null, new LambdaUpdateWrapper<CodeCouponBatch>()
                    .eq(CodeCouponBatch::getId, batchId)
                    .eq(CodeCouponBatch::getVersion, batch.getVersion())
                    .set(CodeCouponBatch::getStatus, 3));
            ThrowUtils.throwIf(rows == 0, ErrorCode.OPERATION_ERROR, "状态更新失败，请重试");

            // 更新 Redis 缓存
            updateBatchInfoCache(batchId, 3);

            log.info("批次已结束 | batchId={}", batchId);

            // 异步将未发放券标记为过期
            asyncExpireUnsoldCoupons(batchId);
        } finally {
            unlock(lockKey);
        }
    }

    // ==================== 异步过期未发放券 ====================

    @Async("asyncTaskExecutor")
    public void asyncExpireUnsoldCoupons(Long batchId) {
        try {
            int updated = couponMapper.update(null, new LambdaUpdateWrapper<CodeCoupon>()
                    .eq(CodeCoupon::getBatchId, batchId)
                    .eq(CodeCoupon::getStatus, 0)
                    .set(CodeCoupon::getStatus, 3));
            log.info("批次未发放券过期处理完成 | batchId={}, expiredCount={}", batchId, updated);
        } catch (Exception e) {
            log.error("批次未发放券过期处理失败 | batchId={}", batchId, e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 创建参数校验
     */
    private void validateCreateParams(BatchCreateDTO dto) {
        ThrowUtils.throwIf(dto == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(dto.getName()), ErrorCode.PARAMS_ERROR, "批次名称不能为空");
        ThrowUtils.throwIf(!VALID_TYPES.contains(dto.getType()), ErrorCode.PARAMS_ERROR, "券类型只能是 3/7/30");
        ThrowUtils.throwIf(dto.getTotalStock() == null || dto.getTotalStock() <= 0, ErrorCode.PARAMS_ERROR, "库存必须大于0");
        ThrowUtils.throwIf(dto.getStartTime() == null, ErrorCode.PARAMS_ERROR, "开始时间不能为空");
        ThrowUtils.throwIf(dto.getStartTime().before(new Date()), ErrorCode.PARAMS_ERROR, "开始时间不能早于当前时间");
        ThrowUtils.throwIf(dto.getEndTime() == null, ErrorCode.PARAMS_ERROR, "结束时间不能为空");
        ThrowUtils.throwIf(!dto.getEndTime().after(dto.getStartTime()), ErrorCode.PARAMS_ERROR, "结束时间必须晚于开始时间");
    }

    /**
     * 状态流转合法性校验
     */
    private void validateTransition(int currentStatus, int targetStatus, CodeCouponBatch batch) {
        // 合法转换矩阵
        if (currentStatus == 0 && targetStatus == 1) {
            // 草稿 -> 预热中：检查兑换码是否已生成
            Long codeCount = couponMapper.selectCount(
                    new LambdaQueryWrapper<CodeCoupon>()
                            .eq(CodeCoupon::getBatchId, batch.getId())
                            .eq(CodeCoupon::getStatus, 0));
            ThrowUtils.throwIf(codeCount == null || codeCount == 0,
                    ErrorCode.OPERATION_ERROR, "请先生成兑换码");
            ThrowUtils.throwIf(!codeCount.equals(Long.valueOf(batch.getTotalStock())),
                    ErrorCode.OPERATION_ERROR, "兑换码数量(" + codeCount + ")与库存(" + batch.getTotalStock() + ")不一致");
            return;
        }

        if (currentStatus == 1 && targetStatus == 2) {
            // 预热中 -> 进行中：检查开始时间
            ThrowUtils.throwIf(new Date().before(batch.getStartTime()),
                    ErrorCode.OPERATION_ERROR, "活动尚未到开始时间");
            return;
        }

        if (currentStatus == 2 && targetStatus == 3) {
            // 进行中 -> 已结束：直接允许
            return;
        }

        throw new BusinessException(ErrorCode.OPERATION_ERROR,
                "不允许的状态转换: " + currentStatus + " -> " + targetStatus);
    }

    /**
     * 更新 Redis 批次详情缓存中的 status
     */
    private void updateBatchInfoCache(Long batchId, int newStatus) {
        String batchInfoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String json = stringRedisTemplate.opsForValue().get(batchInfoKey);
        if (StrUtil.isNotBlank(json)) {
            // 简单替换 status 字段值
            String updated = json.replaceAll("\"status\":\\d+", "\"status\":" + newStatus);
            stringRedisTemplate.opsForValue().set(batchInfoKey, updated);
        }
    }

    /**
     * 清理批次相关的 Redis 缓存
     */
    private void cleanBatchRedisCache(Long batchId) {
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        String infoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        stringRedisTemplate.delete(List.of(stockKey, infoKey));
    }

    /**
     * 补充生成兑换码（库存增大时）
     */
    private void generateExtraCodes(Long batchId, int type, int count, Date endTime) {
        Date expireAt = roundToHour(addDays(endTime, 7));
        List<CodeCoupon> buffer = new ArrayList<>(BATCH_INSERT_SIZE);
        for (int i = 0; i < count; i++) {
            CodeCoupon coupon = new CodeCoupon();
            coupon.setBatchId(batchId);
            coupon.setType(type);
            coupon.setStatus(0);
            coupon.setCode(generateCouponCode(type));
            coupon.setExpireAt(expireAt);
            buffer.add(coupon);

            if (buffer.size() >= BATCH_INSERT_SIZE) {
                batchInsertCoupons(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            batchInsertCoupons(buffer);
        }
    }

    /**
     * 删除多余的未发放券（库存减少时）
     */
    private void deleteExtraUnsoldCoupons(Long batchId, int deleteCount) {
        // 查询要删除的券 ID
        List<CodeCoupon> toDelete = couponMapper.selectList(
                new LambdaQueryWrapper<CodeCoupon>()
                        .eq(CodeCoupon::getBatchId, batchId)
                        .eq(CodeCoupon::getStatus, 0)
                        .last("LIMIT " + deleteCount));
        if (!toDelete.isEmpty()) {
            List<Long> ids = toDelete.stream().map(CodeCoupon::getId).toList();
            couponMapper.deleteBatchIds(ids);
        }
    }

    /**
     * 批量插入券
     */
    private void batchInsertCoupons(List<CodeCoupon> coupons) {
        // 使用 MyBatis-Plus 的 saveBatch 需要 IService，
        // 这里用 Mapper 逐条插入（批量插入需要在 Mapper XML 中自定义）
        for (CodeCoupon coupon : coupons) {
            couponMapper.insert(coupon);
        }
    }

    /**
     * 生成券码: VIP{type}{8位hex}
     */
    private String generateCouponCode(int type) {
        return "VIP" + type + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 生成批次号
     */
    private String generateBatchNo() {
        return "BATCH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", new Random().nextInt(1000));
    }

    private boolean tryLock(String key, long ttlSeconds) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 日期加天数
     */
    private Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    /**
     * 向上取整到下一个整点
     */
    private Date roundToHour(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (cal.get(Calendar.MINUTE) > 0 || cal.get(Calendar.SECOND) > 0 || cal.get(Calendar.MILLISECOND) > 0) {
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
