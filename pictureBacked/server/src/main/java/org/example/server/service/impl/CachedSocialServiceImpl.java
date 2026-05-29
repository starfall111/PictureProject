package org.example.server.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.RedisCacheUtil;
import org.example.pojo.dto.social.UserPictureQueryDTO;
import org.example.pojo.entity.Picture;
import org.example.pojo.entity.PictureFavorite;
import org.example.pojo.entity.PictureLike;
import org.example.pojo.entity.PictureStatistics;
import org.example.pojo.vo.PictureBriefVO;
import org.example.pojo.vo.PictureStatisticsVO;
import org.example.pojo.vo.ToggleFavoriteVO;
import org.example.pojo.vo.ToggleLikeVO;
import org.example.common.constants.RedisKeyConstants;
import org.example.server.mapper.PictureFavoriteMapper;
import org.example.server.mapper.PictureLikeMapper;
import org.example.server.mapper.PictureMapper;
import org.example.server.mapper.PictureStatisticsMapper;
import org.example.server.service.SocialService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存版社交服务实现
 * - 点赞/收藏 → Redis 计数 + DB 实时双写
 * - 浏览/下载/分享 → 仅 Redis INCR，定时同步到 DB
 *
 * @author Zou
 */
@Slf4j
@Service("cachedSocialService")
public class CachedSocialServiceImpl implements SocialService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private PictureLikeMapper pictureLikeMapper;

    @Resource
    private PictureFavoriteMapper pictureFavoriteMapper;

    @Resource
    private PictureStatisticsMapper pictureStatisticsMapper;

    /**
     * 分布式锁超时时间（秒）
     */
    private static final int LOCK_TIMEOUT_SECONDS = 10;

    @Override
    public ToggleLikeVO toggleLike(Long pictureId, Long userId) {
        validPicturePublic(pictureId);

        String lockKey = String.format(RedisKeyConstants.SOCIAL_LOCK_KEY, "like", userId, pictureId);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        ThrowUtils.throwIf(!Boolean.TRUE.equals(locked), ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");

        try {
            String likeKey = String.format(RedisKeyConstants.SOCIAL_LIKE_KEY, userId, pictureId);
            String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);

            // 确保 Hash 已初始化
            initStatsHashIfNeeded(pictureId);

            boolean liked;
            String existing = stringRedisTemplate.opsForValue().get(likeKey);
            if ("1".equals(existing)) {
                // 已点赞 → 取消点赞，value 设为 "0"
                stringRedisTemplate.opsForValue().set(likeKey, "0");
                stringRedisTemplate.opsForHash().increment(statsKey, "likeCount", -1);
                // DB 删除
                QueryWrapper<PictureLike> qw = new QueryWrapper<>();
                qw.eq("pictureId", pictureId).eq("userId", userId);
                pictureLikeMapper.delete(qw);
                liked = false;
            } else {
                // 未点赞 → 点赞，value 设为 "1"
                stringRedisTemplate.opsForValue().set(likeKey, "1");
                stringRedisTemplate.opsForHash().increment(statsKey, "likeCount", 1);
                // DB 插入
                PictureLike pictureLike = new PictureLike();
                pictureLike.setPictureId(pictureId);
                pictureLike.setUserId(userId);
                pictureLikeMapper.insert(pictureLike);
                liked = true;
            }

            // 标记脏数据
            stringRedisTemplate.opsForSet().add(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY, String.valueOf(pictureId));

            // 读取最新计数
            String likeCountStr = (String) stringRedisTemplate.opsForHash().get(statsKey, "likeCount");
            int likeCount = likeCountStr != null ? Integer.parseInt(likeCountStr) : 0;

            ToggleLikeVO result = new ToggleLikeVO();
            result.setLiked(liked);
            result.setLikeCount(Math.max(likeCount, 0));
            return result;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public Map<Long, Boolean> batchLikeStatus(List<Long> pictureIds, Long userId) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (userId == null) {
            return pictureIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        Map<Long, Boolean> result = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();

        // 逐个查询 Redis：有 key 时根据 value "1"=已点赞 "0"=未点赞
        for (Long pictureId : pictureIds) {
            String likeKey = String.format(RedisKeyConstants.SOCIAL_LIKE_KEY, userId, pictureId);
            String value = stringRedisTemplate.opsForValue().get(likeKey);
            if (value != null) {
                result.put(pictureId, "1".equals(value));
            } else {
                missedIds.add(pictureId);
            }
        }

        // 没有 key 的从 DB 查询并回填 Redis
        if (!missedIds.isEmpty()) {
            QueryWrapper<PictureLike> qw = new QueryWrapper<>();
            qw.in("pictureId", missedIds).eq("userId", userId);
            List<PictureLike> likes = pictureLikeMapper.selectList(qw);
            Set<Long> likedIds = likes.stream().map(PictureLike::getPictureId).collect(Collectors.toSet());

            for (Long pictureId : missedIds) {
                boolean liked = likedIds.contains(pictureId);
                result.put(pictureId, liked);
                String likeKey = String.format(RedisKeyConstants.SOCIAL_LIKE_KEY, userId, pictureId);
                stringRedisTemplate.opsForValue().set(likeKey, liked ? "1" : "0");
            }
        }

        return result;
    }

    @Override
    public ToggleFavoriteVO toggleFavorite(Long pictureId, Long userId) {
        validPicturePublic(pictureId);

        String lockKey = String.format(RedisKeyConstants.SOCIAL_LOCK_KEY, "fav", userId, pictureId);
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        ThrowUtils.throwIf(!Boolean.TRUE.equals(locked), ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");

        try {
            String favKey = String.format(RedisKeyConstants.SOCIAL_FAV_KEY, userId, pictureId);
            String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);

            // 确保 Hash 已初始化
            initStatsHashIfNeeded(pictureId);

            boolean favorited;
            String existing = stringRedisTemplate.opsForValue().get(favKey);
            if ("1".equals(existing)) {
                // 已收藏 → 取消收藏，value 设为 "0"
                stringRedisTemplate.opsForValue().set(favKey, "0");
                stringRedisTemplate.opsForHash().increment(statsKey, "favoriteCount", -1);
                // DB 删除
                QueryWrapper<PictureFavorite> qw = new QueryWrapper<>();
                qw.eq("pictureId", pictureId).eq("userId", userId);
                pictureFavoriteMapper.delete(qw);
                favorited = false;
            } else {
                // 未收藏 → 收藏，value 设为 "1"
                stringRedisTemplate.opsForValue().set(favKey, "1");
                stringRedisTemplate.opsForHash().increment(statsKey, "favoriteCount", 1);
                // DB 插入
                PictureFavorite pictureFavorite = new PictureFavorite();
                pictureFavorite.setPictureId(pictureId);
                pictureFavorite.setUserId(userId);
                pictureFavoriteMapper.insert(pictureFavorite);
                favorited = true;
            }

            // 标记脏数据
            stringRedisTemplate.opsForSet().add(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY, String.valueOf(pictureId));

            // 读取最新计数
            String favCountStr = (String) stringRedisTemplate.opsForHash().get(statsKey, "favoriteCount");
            int favoriteCount = favCountStr != null ? Integer.parseInt(favCountStr) : 0;

            ToggleFavoriteVO result = new ToggleFavoriteVO();
            result.setFavorited(favorited);
            result.setFavoriteCount(Math.max(favoriteCount, 0));
            return result;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public Map<Long, Boolean> batchFavoriteStatus(List<Long> pictureIds, Long userId) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (userId == null) {
            return pictureIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        Map<Long, Boolean> result = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();

        // 逐个查询 Redis：有 key 时根据 value "1"=已收藏 "0"=未收藏
        for (Long pictureId : pictureIds) {
            String favKey = String.format(RedisKeyConstants.SOCIAL_FAV_KEY, userId, pictureId);
            String value = stringRedisTemplate.opsForValue().get(favKey);
            if (value != null) {
                result.put(pictureId, "1".equals(value));
            } else {
                missedIds.add(pictureId);
            }
        }

        // 没有 key 的从 DB 查询并回填 Redis
        if (!missedIds.isEmpty()) {
            QueryWrapper<PictureFavorite> qw = new QueryWrapper<>();
            qw.in("pictureId", missedIds).eq("userId", userId);
            List<PictureFavorite> favorites = pictureFavoriteMapper.selectList(qw);
            Set<Long> favIds = favorites.stream().map(PictureFavorite::getPictureId).collect(Collectors.toSet());

            for (Long pictureId : missedIds) {
                boolean favorited = favIds.contains(pictureId);
                result.put(pictureId, favorited);
                String favKey = String.format(RedisKeyConstants.SOCIAL_FAV_KEY, userId, pictureId);
                stringRedisTemplate.opsForValue().set(favKey, favorited ? "1" : "0");
            }
        }

        return result;
    }

    @Override
    public void recordShare(Long pictureId) {
        validPicturePublic(pictureId);
        incrementStatsField(pictureId, "shareCount");
    }

    @Override
    public void incrementViewCount(Long pictureId) {
        incrementStatsField(pictureId, "viewCount");
    }

    @Override
    public void incrementDownloadCount(Long pictureId) {
        incrementStatsField(pictureId, "downloadCount");
    }

    @Override
    public Map<Long, PictureStatisticsVO> batchStatistics(List<Long> pictureIds) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, PictureStatisticsVO> result = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();

        // Pipeline 批量 HMGET
        for (Long pictureId : pictureIds) {
            String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(statsKey);
            if (!entries.isEmpty()) {
                result.put(pictureId, hashToStatsVO(entries));
            } else {
                missedIds.add(pictureId);
            }
        }

        // 未命中的从 DB 查询并初始化 Redis Hash
        if (!missedIds.isEmpty()) {
            QueryWrapper<PictureStatistics> qw = new QueryWrapper<>();
            qw.in("pictureId", missedIds);
            List<PictureStatistics> stats = pictureStatisticsMapper.selectList(qw);
            Map<Long, PictureStatistics> dbMap = stats.stream()
                    .collect(Collectors.toMap(PictureStatistics::getPictureId, s -> s));

            for (Long pictureId : missedIds) {
                PictureStatistics stat = dbMap.get(pictureId);
                if (stat != null) {
                    // 写入 Redis Hash
                    String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);
                    Map<String, String> hash = new HashMap<>();
                    hash.put("likeCount", String.valueOf(stat.getLikeCount() != null ? stat.getLikeCount() : 0));
                    hash.put("favoriteCount", String.valueOf(stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0));
                    hash.put("shareCount", String.valueOf(stat.getShareCount() != null ? stat.getShareCount() : 0));
                    hash.put("viewCount", String.valueOf(stat.getViewCount() != null ? stat.getViewCount() : 0));
                    hash.put("downloadCount", String.valueOf(stat.getDownloadCount() != null ? stat.getDownloadCount() : 0));
                    stringRedisTemplate.opsForHash().putAll(statsKey, hash);

                    PictureStatisticsVO vo = new PictureStatisticsVO();
                    vo.setLikeCount(stat.getLikeCount() != null ? stat.getLikeCount() : 0);
                    vo.setFavoriteCount(stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0);
                    vo.setShareCount(stat.getShareCount() != null ? stat.getShareCount() : 0);
                    vo.setViewCount(stat.getViewCount() != null ? stat.getViewCount() : 0);
                    vo.setDownloadCount(stat.getDownloadCount() != null ? stat.getDownloadCount() : 0);
                    result.put(pictureId, vo);
                } else {
                    // DB 中也无数据，初始化为零
                    initStatsHash(pictureId, 0, 0, 0, 0, 0);
                    result.put(pictureId, defaultStats());
                }
            }
        }

        return result;
    }

    @Override
    public Page<PictureBriefVO> getUserLikedPictures(Long userId, UserPictureQueryDTO queryDTO) {
        // 委托给 dbSocialService（多表 JOIN + 分页，不走缓存）
        throw new UnsupportedOperationException("请使用 dbSocialService");
    }

    @Override
    public Page<PictureBriefVO> getUserFavoritedPictures(Long userId, UserPictureQueryDTO queryDTO) {
        throw new UnsupportedOperationException("请使用 dbSocialService");
    }

    // ==================== 私有方法 ====================

    private void validPicturePublic(Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片 id 不合法");
        Picture picture = pictureMapper.selectById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(picture.getSpaceId() != null, ErrorCode.NO_AUTH_ERROR, "仅公共图库支持社交功能");
    }

    /**
     * 递增指定统计字段（仅 Redis），并标记脏集合
     */
    private void incrementStatsField(Long pictureId, String field) {
        validPicturePublic(pictureId);
        String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);

        // 确保 Hash 已初始化
        initStatsHashIfNeeded(pictureId);

        stringRedisTemplate.opsForHash().increment(statsKey, field, 1);

        // 标记脏数据
        stringRedisTemplate.opsForSet().add(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY, String.valueOf(pictureId));
    }

    /**
     * 如果 Redis Hash 不存在，从 DB 初始化
     */
    private void initStatsHashIfNeeded(Long pictureId) {
        String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);
        Boolean exists = stringRedisTemplate.hasKey(statsKey);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        PictureStatistics stat = pictureStatisticsMapper.selectById(pictureId);
        if (stat != null) {
            Map<String, String> hash = new HashMap<>();
            hash.put("likeCount", String.valueOf(stat.getLikeCount() != null ? stat.getLikeCount() : 0));
            hash.put("favoriteCount", String.valueOf(stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0));
            hash.put("shareCount", String.valueOf(stat.getShareCount() != null ? stat.getShareCount() : 0));
            hash.put("viewCount", String.valueOf(stat.getViewCount() != null ? stat.getViewCount() : 0));
            hash.put("downloadCount", String.valueOf(stat.getDownloadCount() != null ? stat.getDownloadCount() : 0));
            stringRedisTemplate.opsForHash().putAll(statsKey, hash);
        } else {
            initStatsHash(pictureId, 0, 0, 0, 0, 0);
        }
    }

    /**
     * 初始化 Redis Hash 为零值
     */
    private void initStatsHash(Long pictureId, int like, int fav, int share, int view, int download) {
        String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);
        Map<String, String> hash = new HashMap<>();
        hash.put("likeCount", String.valueOf(like));
        hash.put("favoriteCount", String.valueOf(fav));
        hash.put("shareCount", String.valueOf(share));
        hash.put("viewCount", String.valueOf(view));
        hash.put("downloadCount", String.valueOf(download));
        stringRedisTemplate.opsForHash().putAll(statsKey, hash);
    }

    /**
     * Redis Hash 条目 → PictureStatisticsVO
     */
    private PictureStatisticsVO hashToStatsVO(Map<Object, Object> entries) {
        PictureStatisticsVO vo = new PictureStatisticsVO();
        vo.setLikeCount(parseInt(entries.get("likeCount")));
        vo.setFavoriteCount(parseInt(entries.get("favoriteCount")));
        vo.setShareCount(parseInt(entries.get("shareCount")));
        vo.setViewCount(parseInt(entries.get("viewCount")));
        vo.setDownloadCount(parseInt(entries.get("downloadCount")));
        return vo;
    }

    private int parseInt(Object val) {
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private PictureStatisticsVO defaultStats() {
        PictureStatisticsVO vo = new PictureStatisticsVO();
        vo.setLikeCount(0);
        vo.setFavoriteCount(0);
        vo.setShareCount(0);
        vo.setViewCount(0);
        vo.setDownloadCount(0);
        return vo;
    }
}
