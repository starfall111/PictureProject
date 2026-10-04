package org.example.picture.moderation.application.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.identity.api.UserContext;
import org.example.shared.contract.NotificationTypeEnum;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.picture.moderation.interfaces.dto.BanUnbanDTO;
import org.example.picture.moderation.domain.model.BanRecord;
import org.example.identity.api.model.User;
import org.example.picture.moderation.interfaces.vo.BanRecordVO;
import org.example.picture.moderation.infrastructure.persistence.BanRecordMapper;
import org.example.identity.infrastructure.persistence.UserMapper;
import org.example.picture.moderation.application.BanService;
import org.example.identity.application.UserService;
import org.example.shared.contract.NotificationEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 封禁服务实现
 *
 * @author Zou
 */
@Slf4j
@Service
public class BanServiceImpl extends ServiceImpl<BanRecordMapper, BanRecord>
        implements BanService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    @Qualifier("dbUserService")
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    // ==================== 核心方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeBan(Long userId, String banType, Integer banDuration, String banReason, Long reportId) {
        User currentAdmin = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentAdmin), ErrorCode.NOT_LOGIN_ERROR);

        // 1. 获取被封禁用户
        User bannedUser = userService.getById(userId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(bannedUser), ErrorCode.NOT_FOUND_ERROR, "被封禁用户不存在");

        // 1.5 检查是否已被封禁，防止重复封禁
        String currentBanStatus = bannedUser.getBanStatus();
        if (StrUtil.isNotBlank(currentBanStatus) && !"NONE".equals(currentBanStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户已被封禁，不能重复封禁");
        }

        // 2. 计算封禁结束时间
        Date now = new Date();
        Date banEndTime = null;
        if (!"BAN_PERMANENT".equals(banType) && banDuration != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.add(Calendar.DAY_OF_MONTH, banDuration);
            banEndTime = calendar.getTime();
        }

        // 3. 创建封禁记录
        BanRecord banRecord = new BanRecord();
        banRecord.setUserId(userId);
        banRecord.setBanType(banType);
        banRecord.setBanDuration(banDuration);
        banRecord.setBanStartTime(now);
        banRecord.setBanEndTime(banEndTime);
        banRecord.setBanReason(banReason);
        banRecord.setViolationCount(bannedUser.getViolationCount() != null ? bannedUser.getViolationCount() : 0);
        banRecord.setUnbanned(0);
        banRecord.setBanOperatorId(currentAdmin.getId());
        banRecord.setBanOperatorName(currentAdmin.getUserName());
        banRecord.setReportId(reportId);
        banRecord.setCreateTime(now);
        banRecord.setUpdateTime(now);

        boolean saved = this.save(banRecord);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "创建封禁记录失败");

        // 4. 更新用户封禁状态
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setBanStatus(banType);
        updateUser.setBanEndTime(banEndTime);
        // 递增违规次数
        int currentViolationCount = bannedUser.getViolationCount() != null ? bannedUser.getViolationCount() : 0;
        updateUser.setViolationCount(currentViolationCount + 1);
        updateUser.setLastViolationTime(now);
        boolean userUpdated = userService.updateById(updateUser);
        ThrowUtils.throwIf(!userUpdated, ErrorCode.SYSTEM_ERROR, "更新用户封禁状态失败");

        // 5. 写入 Redis 黑名单 ZSET（PERMANENT 用 score=0，永不被自动解封扫描到）
        long banEndTimestamp = banEndTime != null ? banEndTime.getTime() : 0L;
        stringRedisTemplate.opsForZSet().add(RedisKeyConstants.BAN_BLACKLIST_ZSET,
                String.valueOf(userId), banEndTimestamp);

        // 6. 写入 Redis 封禁状态缓存
        writeBanStatusCache(userId, banType, banEndTime);

        // 7. 失效用户缓存
        invalidateUserCache(userId);

        // 8. 发送封禁通知
        String banDesc = getBanTypeDesc(banType, banDuration);
        publishBanNotification(userId, currentAdmin, banDesc, banReason, reportId);

        log.info("管理员 {} 封禁用户 {}: banType={}, duration={}天, reportId={}",
                currentAdmin.getId(), userId, banType, banDuration, reportId);

        // 失效封禁统计缓存
        invalidateBanStatsCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(BanUnbanDTO dto) {
        User currentAdmin = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentAdmin), ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(dto.getUserId()), ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        Long userId = dto.getUserId();

        // 1. 查找活跃封禁记录
        List<BanRecord> activeRecords = baseMapper.selectActiveByUserId(userId);
        ThrowUtils.throwIf(ObjUtil.isEmpty(activeRecords), ErrorCode.OPERATION_ERROR, "该用户没有生效的封禁记录");

        Date now = new Date();

        // 2. 更新所有活跃封禁记录为已解封
        for (BanRecord record : activeRecords) {
            BanRecord updateRecord = new BanRecord();
            updateRecord.setId(record.getId());
            updateRecord.setUnbanned(1);
            updateRecord.setUnbanTime(now);
            updateRecord.setUnbanReason(dto.getUnbanReason());
            updateRecord.setUnbanOperatorId(currentAdmin.getId());
            updateRecord.setUpdateTime(now);
            this.updateById(updateRecord);
        }

        // 3. 更新用户状态
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setBanStatus("NONE");
        updateUser.setBanEndTime(null);
        userService.updateById(updateUser);

        // 4. 从 Redis 黑名单 ZSET 中移除
        stringRedisTemplate.opsForZSet().remove(RedisKeyConstants.BAN_BLACKLIST_ZSET, String.valueOf(userId));

        // 5. 删除 Redis 封禁状态缓存
        String banStatusKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, userId);
        stringRedisTemplate.delete(banStatusKey);

        // 6. 失效用户缓存
        invalidateUserCache(userId);

        // 7. 发送解封通知
        publishUnbanNotification(userId, currentAdmin, dto.getUnbanReason());

        log.info("管理员 {} 手动解封用户 {}: reason={}", currentAdmin.getId(), userId, dto.getUnbanReason());

        // 失效封禁统计缓存
        invalidateBanStatsCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoUnbanExpiredUsers() {
        long currentTimestamp = System.currentTimeMillis();

        // 1. 查找所有到期用户（score <= 当前时间戳，排除 score=0 的永久封禁）
        Set<String> expiredUserIds = stringRedisTemplate.opsForZSet()
                .rangeByScore(RedisKeyConstants.BAN_BLACKLIST_ZSET, 1, currentTimestamp);

        if (ObjUtil.isEmpty(expiredUserIds)) {
            return;
        }

        log.info("自动解封扫描: 发现 {} 个到期用户", expiredUserIds.size());

        Date now = new Date();
        for (String userIdStr : expiredUserIds) {
            try {
                Long userId = Long.parseLong(userIdStr);

                // 查找活跃封禁记录
                List<BanRecord> activeRecords = baseMapper.selectActiveByUserId(userId);
                if (ObjUtil.isNotEmpty(activeRecords)) {
                    for (BanRecord record : activeRecords) {
                        BanRecord updateRecord = new BanRecord();
                        updateRecord.setId(record.getId());
                        updateRecord.setUnbanned(1);
                        updateRecord.setUnbanTime(now);
                        updateRecord.setUnbanReason("临时封禁到期自动解封");
                        updateRecord.setUpdateTime(now);
                        this.updateById(updateRecord);
                    }
                }

                // 更新用户状态
                User updateUser = new User();
                updateUser.setId(userId);
                updateUser.setBanStatus("NONE");
                updateUser.setBanEndTime(null);
                userService.updateById(updateUser);

                // 删除缓存
                String banStatusKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, userId);
                stringRedisTemplate.delete(banStatusKey);
                invalidateUserCache(userId);

                log.info("自动解封用户: userId={}", userId);
            } catch (Exception e) {
                log.error("自动解封失败: userId={}", userIdStr, e);
            }
        }

        // 清理已处理的条目
        stringRedisTemplate.opsForZSet().removeRangeByScore(RedisKeyConstants.BAN_BLACKLIST_ZSET, 1, currentTimestamp);

        // 失效封禁统计缓存
        if (ObjUtil.isNotEmpty(expiredUserIds)) {
            invalidateBanStatsCache();
        }
    }

    @Override
    public void checkBanStatus(User user) {
        if (ObjUtil.isEmpty(user) || ObjUtil.isEmpty(user.getId())) {
            return;
        }

        Long userId = user.getId();

        try {
            // 1. 尝试从 Redis 缓存获取封禁状态
            String banStatusKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, userId);
            String cachedStatus = stringRedisTemplate.opsForValue().get(banStatusKey);

            if (StrUtil.isNotBlank(cachedStatus)) {
                handleCachedBanStatus(user, cachedStatus);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis 查询封禁状态异常，降级到数据库查询: userId={}", userId, e);
        }

        // 2. 缓存未命中或 Redis 异常，查询数据库
        User dbUser = userService.getById(userId);
        if (ObjUtil.isEmpty(dbUser)) {
            return;
        }

        String banStatus = dbUser.getBanStatus();
        if (StrUtil.isBlank(banStatus) || "NONE".equals(banStatus)) {
            // 写入无封禁的缓存
            writeBanStatusCache(userId, "NONE", null);
            return;
        }

        // 检查临时封禁是否已过期
        if (isTempBan(banStatus) && dbUser.getBanEndTime() != null) {
            if (dbUser.getBanEndTime().before(new Date())) {
                // 已过期，自动解封
                autoUnbanSingleUser(userId);
                return;
            }
            throw new BusinessException(ErrorCode.USER_BANNED, "账号已被临时封禁，解封时间: " + dbUser.getBanEndTime());
        }

        if ("BAN_PERMANENT".equals(banStatus)) {
            throw new BusinessException(ErrorCode.USER_BANNED_PERMANENT);
        }

        // 其他封禁类型
        throw new BusinessException(ErrorCode.USER_BANNED);
    }

    @Override
    public Page<BanRecordVO> getBanRecordList(int current, int pageSize, String banStatus) {
        QueryWrapper<BanRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0);

        if (StrUtil.isNotBlank(banStatus)) {
            if ("ACTIVE".equals(banStatus)) {
                queryWrapper.eq("unbanned", 0);
            } else if ("UNBANNED".equals(banStatus)) {
                queryWrapper.eq("unbanned", 1);
            }
        }

        queryWrapper.orderByDesc("createTime");

        Page<BanRecord> recordPage = this.page(new Page<>(current, pageSize), queryWrapper);
        return convertToVOPage(recordPage);
    }

    @Override
    public Map<String, Object> getBanStats() {
        // 先查缓存
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisKeyConstants.BAN_STATS_KEY);
            if (StrUtil.isNotBlank(cached)) {
                return JSONUtil.toBean(cached, Map.class);
            }
        } catch (Exception e) {
            log.warn("读取封禁统计缓存失败", e);
        }

        Map<String, Object> stats = new HashMap<>();

        // 总封禁记录数
        long totalRecords = this.count(new QueryWrapper<BanRecord>().eq("isDelete", 0));
        stats.put("totalRecords", totalRecords);

        // 当前活跃封禁数
        long activeBanned = this.count(new QueryWrapper<BanRecord>().eq("unbanned", 0).eq("isDelete", 0));
        stats.put("activeBanned", activeBanned);

        // 临时封禁数
        long tempBanned = this.count(new QueryWrapper<BanRecord>()
                .eq("unbanned", 0).eq("isDelete", 0)
                .in("banType", "BAN_TEMP_3", "BAN_TEMP_7", "BAN_TEMP_30"));
        stats.put("tempBanned", tempBanned);

        // 永久封禁数
        long permanentBanned = this.count(new QueryWrapper<BanRecord>()
                .eq("unbanned", 0).eq("isDelete", 0)
                .eq("banType", "BAN_PERMANENT"));
        stats.put("permanentBanned", permanentBanned);

        // 本月解封数
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthlyUnbanned = this.count(new QueryWrapper<BanRecord>()
                .eq("unbanned", 1).eq("isDelete", 0)
                .ge("unbanTime", cal.getTime()));
        stats.put("monthlyUnbanned", monthlyUnbanned);

        // 写入缓存
        try {
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.BAN_STATS_KEY,
                    JSONUtil.toJsonStr(stats), RedisKeyConstants.BAN_STATS_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入封禁统计缓存失败", e);
        }

        return stats;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 失效封禁统计缓存
     */
    private void invalidateBanStatsCache() {
        try {
            stringRedisTemplate.delete(RedisKeyConstants.BAN_STATS_KEY);
            log.debug("Invalidated ban stats cache");
        } catch (Exception e) {
            log.warn("失效封禁统计缓存失败", e);
        }
    }

    /**
     * 处理 Redis 缓存中的封禁状态
     */
    private void handleCachedBanStatus(User user, String cachedStatus) {
        try {
            Map<String, Object> statusMap = JSONUtil.toBean(cachedStatus, Map.class);
            String banStatus = (String) statusMap.get("banStatus");

            if ("NONE".equals(banStatus) || StrUtil.isBlank(banStatus)) {
                return;
            }

            // 检查临时封禁是否到期
            if (isTempBan(banStatus)) {
                String banEndTimeStr = (String) statusMap.get("banEndTime");
                if (StrUtil.isNotBlank(banEndTimeStr)) {
                    long banEndTime = Long.parseLong(banEndTimeStr);
                    if (banEndTime <= System.currentTimeMillis()) {
                        // 已到期，触发自动解封
                        autoUnbanSingleUser(user.getId());
                        return;
                    }
                    throw new BusinessException(ErrorCode.USER_BANNED,
                            "账号已被临时封禁，解封时间: " + new Date(banEndTime));
                }
            }

            if ("BAN_PERMANENT".equals(banStatus)) {
                throw new BusinessException(ErrorCode.USER_BANNED_PERMANENT);
            }

            throw new BusinessException(ErrorCode.USER_BANNED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析封禁状态缓存失败: userId={}", user.getId(), e);
        }
    }

    /**
     * 判断是否为临时封禁
     */
    private boolean isTempBan(String banStatus) {
        return "BAN_TEMP_3".equals(banStatus) || "BAN_TEMP_7".equals(banStatus)
                || "BAN_TEMP_30".equals(banStatus);
    }

    /**
     * 单个用户自动解封（checkBanStatus 中调用）
     */
    private void autoUnbanSingleUser(Long userId) {
        try {
            List<BanRecord> activeRecords = baseMapper.selectActiveByUserId(userId);
            Date now = new Date();

            if (ObjUtil.isNotEmpty(activeRecords)) {
                for (BanRecord record : activeRecords) {
                    BanRecord updateRecord = new BanRecord();
                    updateRecord.setId(record.getId());
                    updateRecord.setUnbanned(1);
                    updateRecord.setUnbanTime(now);
                    updateRecord.setUnbanReason("临时封禁到期自动解封");
                    updateRecord.setUpdateTime(now);
                    this.updateById(updateRecord);
                }
            }

            // 更新用户状态
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setBanStatus("NONE");
            updateUser.setBanEndTime(null);
            userService.updateById(updateUser);

            // 清理缓存
            stringRedisTemplate.opsForZSet().remove(RedisKeyConstants.BAN_BLACKLIST_ZSET, String.valueOf(userId));
            String banStatusKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, userId);
            stringRedisTemplate.delete(banStatusKey);
            invalidateUserCache(userId);

            log.info("登录时自动解封用户: userId={}", userId);
        } catch (Exception e) {
            log.error("自动解封单个用户失败: userId={}", userId, e);
        }
    }

    /**
     * 写入封禁状态缓存
     */
    private void writeBanStatusCache(Long userId, String banStatus, Date banEndTime) {
        try {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("banStatus", banStatus);
            statusMap.put("banEndTime", banEndTime != null ? String.valueOf(banEndTime.getTime()) : null);

            String banStatusKey = String.format(RedisKeyConstants.BAN_STATUS_KEY, userId);
            stringRedisTemplate.opsForValue().set(banStatusKey, JSONUtil.toJsonStr(statusMap),
                    RedisKeyConstants.BAN_STATUS_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入封禁状态缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 失效用户缓存
     */
    private void invalidateUserCache(Long userId) {
        try {
            String userInfoKey = String.format(RedisKeyConstants.USER_INFO_KEY, userId);
            String userProfileKey = String.format(RedisKeyConstants.USER_PROFILE_KEY, userId);
            stringRedisTemplate.delete(Arrays.asList(userInfoKey, userProfileKey));
        } catch (Exception e) {
            log.warn("失效用户缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 发送封禁通知
     */
    private void publishBanNotification(Long userId, User admin, String banDesc, String banReason, Long reportId) {
        try {
            String content = "您的账号已被" + banDesc + "。原因：" + banReason;
            if (reportId != null) {
                content += "。如有疑问，请联系管理员。";
            }
            eventPublisher.publishEvent(new NotificationEvent(
                    this, userId, admin.getId(), admin.getUserName(), admin.getUserAvatar(),
                    NotificationTypeEnum.USER_BANNED, "账号封禁通知", content,
                    reportId, null
            ));
        } catch (Exception e) {
            log.warn("发送封禁通知失败: userId={}", userId, e);
        }
    }

    /**
     * 发送解封通知
     */
    private void publishUnbanNotification(Long userId, User admin, String unbanReason) {
        try {
            String content = "您的账号已解除封禁" +
                    (StrUtil.isNotBlank(unbanReason) ? "。原因：" + unbanReason : "") +
                    "。请遵守社区规范。";
            eventPublisher.publishEvent(new NotificationEvent(
                    this, userId, admin.getId(), admin.getUserName(), admin.getUserAvatar(),
                    NotificationTypeEnum.USER_UNBANNED, "账号解封通知", content,
                    null, null
            ));
        } catch (Exception e) {
            log.warn("发送解封通知失败: userId={}", userId, e);
        }
    }

    /**
     * 获取封禁类型描述
     */
    private String getBanTypeDesc(String banType, Integer banDuration) {
        if ("BAN_PERMANENT".equals(banType)) {
            return "永久封禁";
        }
        if (banDuration != null) {
            return "临时封禁" + banDuration + "天";
        }
        return "封禁";
    }

    /**
     * 分页实体转分页 VO
     */
    private Page<BanRecordVO> convertToVOPage(Page<BanRecord> recordPage) {
        Page<BanRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<BanRecordVO> voList = recordPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 实体转 VO
     */
    private BanRecordVO convertToVO(BanRecord record) {
        if (ObjUtil.isEmpty(record)) {
            return null;
        }
        BanRecordVO vo = new BanRecordVO();
        BeanUtil.copyProperties(record, vo);

        // 填充用户名
        if (record.getUserId() != null) {
            try {
                User bannedUser = userService.getById(record.getUserId());
                if (ObjUtil.isNotEmpty(bannedUser)) {
                    vo.setUserName(bannedUser.getUserName());
                }
            } catch (Exception e) {
                log.warn("查询被封禁用户信息失败: userId={}", record.getUserId(), e);
            }
        }

        // 封禁类型描述
        vo.setBanTypeDesc(getBanTypeDesc(record.getBanType(), record.getBanDuration()));

        // 是否已解封
        vo.setUnbanned(record.getUnbanned() != null && record.getUnbanned() == 1);

        return vo;
    }
}
