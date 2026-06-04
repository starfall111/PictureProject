package org.example.server.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.RedisCacheUtil;
import org.example.pojo.dto.feed.FeedQueryDTO;
import org.example.pojo.vo.FeedTimelineVO;
import org.example.pojo.vo.FeedUnreadVO;
import org.example.pojo.vo.FeedVO;
import org.example.server.mapper.PictureMapper;
import org.example.server.service.FeedService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 动态 Feed 服务缓存装饰器实现
 * <p>
 * 缓存策略：
 * - 每个页码独立缓存（key = feed:page:{userId}:{current}:{pageSize}）
 * - 缓存不存在时回退到 DB 查询并写入缓存
 * - 使用 RedisCacheUtil.getWithLock 防击穿
 * - markRead 时：更新水位线 + 删除该用户所有分页缓存
 *
 * @author Zou
 */
@Slf4j
@Service("cachedFeedService")
public class CachedFeedServiceImpl implements FeedService {

    /**
     * 动态分页缓存 key：feed:page:{userId}:{current}:{pageSize}
     */
    private static final String FEED_PAGE_CACHE_KEY = "feed:page:%d:%d:%d";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    private PictureMapper pictureMapper;

    @Resource(name = "dbFeedService")
    private FeedService dbFeedService;

    @Override
    public FeedTimelineVO getTimeline(Long userId, FeedQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");

        int current = queryDTO.getCurrent();
        int pageSize = queryDTO.getPageSize();
        ThrowUtils.throwIf(current < 1, ErrorCode.PARAMS_ERROR, "页码不合法");
        ThrowUtils.throwIf(pageSize < 1 || pageSize > 50, ErrorCode.PARAMS_ERROR, "每页数量不合法");

        String cacheKey = String.format(FEED_PAGE_CACHE_KEY, userId, current, pageSize);
        int ttl = RedisKeyConstants.FEED_CACHE_TTL_BASE
                + RandomUtil.randomInt(0, RedisKeyConstants.FEED_CACHE_TTL_JITTER + 1);

        String json = redisCacheUtil.getWithLock(cacheKey, ttl, () -> {
            // 缓存未命中 -> 查询 DB
            FeedTimelineVO result = dbFeedService.getTimeline(userId, queryDTO);
            Date watermark = getWatermark(userId);
            Long unreadCount = pictureMapper.countFeedUnread(userId, watermark);
            result.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
            markNewItems(result.getRecords(), watermark);
            return JSONUtil.toJsonStr(result);
        });

        if (json == null) {
            return new FeedTimelineVO();
        }
        return JSONUtil.toBean(json, FeedTimelineVO.class);
    }

    @Override
    public FeedUnreadVO getUnreadCount(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");

        Date watermark = getWatermark(userId);
        Long unreadCount = pictureMapper.countFeedUnread(userId, watermark);
        FeedUnreadVO vo = new FeedUnreadVO();
        vo.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
        return vo;
    }

    @Override
    public void markRead(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");

        // 1. 更新水位线为当前时间
        String watermarkKey = String.format(RedisKeyConstants.FEED_WATERMARK_KEY, userId);
        stringRedisTemplate.opsForValue().set(
                watermarkKey,
                String.valueOf(System.currentTimeMillis()),
                RedisKeyConstants.FEED_WATERMARK_TTL, TimeUnit.SECONDS
        );

        // 2. 删除该用户所有分页缓存
        String pattern = "feed:page:" + userId + ":*";
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        log.info("用户 {} 标记动态已读，水位线更新为 {}", userId, System.currentTimeMillis());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取用户阅读水位线
     */
    private Date getWatermark(Long userId) {
        String watermarkKey = String.format(RedisKeyConstants.FEED_WATERMARK_KEY, userId);
        String value = stringRedisTemplate.opsForValue().get(watermarkKey);
        if (value != null) {
            try {
                Long watermark = Long.parseLong(value);
                return new Date(watermark);
            } catch (NumberFormatException e) {
                log.warn("水位线格式异常: userId={}, value={}", userId, value);
            }
        }
        // 默认水位线为当前时间（所有动态都是未读）
        return new Date(System.currentTimeMillis());
    }

    /**
     * 为 DB 查询结果标记 isNew
     */
    private void markNewItems(List<FeedVO> records, Date watermark) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (FeedVO vo : records) {
            if (vo.getCreateTime() != null) {
                vo.setIsNew(vo.getCreateTime().getTime() > watermark.getTime());
            }
        }
    }
}
