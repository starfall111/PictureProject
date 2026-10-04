package org.example.picture.social.application.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.annotation.RedisTimed;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.util.RedisCacheUtil;
import org.example.picture.social.interfaces.dto.SocialActionMessage;
import org.example.picture.social.interfaces.dto.UserPictureQueryDTO;
import org.example.picture.core.domain.model.Picture;
import org.example.picture.social.domain.model.PictureFavorite;
import org.example.picture.social.domain.model.PictureLike;
import org.example.picture.core.domain.model.PictureStatistics;
import org.example.picture.core.interfaces.vo.PictureBriefVO;
import org.example.picture.core.interfaces.vo.PictureStatisticsVO;
import org.example.picture.social.interfaces.vo.ToggleFavoriteVO;
import org.example.picture.social.interfaces.vo.ToggleLikeVO;
import org.example.shared.constants.RedisKeyConstants;
import org.example.picture.social.infrastructure.mq.SocialActionMQConfig;
import org.example.picture.social.infrastructure.persistence.PictureFavoriteMapper;
import org.example.picture.social.infrastructure.persistence.PictureLikeMapper;
import org.example.picture.core.infrastructure.persistence.PictureMapper;
import org.example.picture.core.infrastructure.persistence.PictureStatisticsMapper;
import org.example.picture.social.application.SocialService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存版社交服务实现
 * - 点赞/收藏 → Redis 同步更新（用户即时响应）+ RabbitMQ 异步写入 DB + 发布通知
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

    @Resource(name = "dbSocialService")
    private SocialService dbSocialService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 点赞/取消点赞（toggle）
     * <p>
     * Redis 操作同步执行（用户即刻获得响应），DB 写入通过 RabbitMQ 异步处理。
     * 快速重复点击场景：Redis 分布式锁保证串行化，DB 唯一索引冲突由 Consumer 端静默忽略。
     * </p>
     */
    @Override
    @RedisTimed(value = "toggleLike", warnThreshold = 100)
    public ToggleLikeVO toggleLike(Long pictureId, Long userId) {
        Picture picture = validPicturePublic(pictureId);

        // ── 1. 分布式锁（需同步获取结果；RLock 看门狗续期，快速连点在等待期快速失败） ──
        String lockKey = String.format(RedisKeyConstants.SOCIAL_LOCK_KEY, "like", userId, pictureId);
        return redisCacheUtil.executeWithLock(lockKey, () -> {
            String likeKey = String.format(RedisKeyConstants.SOCIAL_LIKE_KEY, userId, pictureId);
            String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);

            // ── 2. Pipeline：批量读取当前状态（1 RTT） ──
            List<Object> readResults = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) throws DataAccessException {
                    operations.opsForValue().get(likeKey);
                    operations.hasKey(statsKey);
                    return null;
                }
            });

            String existing = (String) readResults.get(0);
            boolean statsInitialized = Boolean.TRUE.equals(readResults.get(1));

            boolean liked;
            String newValue;
            long delta;
            if ("1".equals(existing)) {
                liked = false;
                newValue = "0";
                delta = -1;
            } else {
                liked = true;
                newValue = "1";
                delta = 1;
            }

            // ── 3. Hash 未初始化时从 DB 加载（非 Redis 操作） ──
            final boolean needInit = !statsInitialized;
            Map<String, String> initFields = null;
            int statsTtl = 0;
            if (needInit) {
                PictureStatistics stat = pictureStatisticsMapper.selectById(pictureId);
                initFields = new LinkedHashMap<>();
                initFields.put("likeCount", String.valueOf(stat != null && stat.getLikeCount() != null ? stat.getLikeCount() : 0));
                initFields.put("favoriteCount", String.valueOf(stat != null && stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0));
                initFields.put("shareCount", String.valueOf(stat != null && stat.getShareCount() != null ? stat.getShareCount() : 0));
                initFields.put("viewCount", String.valueOf(stat != null && stat.getViewCount() != null ? stat.getViewCount() : 0));
                initFields.put("downloadCount", String.valueOf(stat != null && stat.getDownloadCount() != null ? stat.getDownloadCount() : 0));
                statsTtl = RedisKeyConstants.SOCIAL_STATS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATS_TTL_JITTER);
            }

            // ── 4. Pipeline：批量写入 + 读取最终计数（1 RTT） ──
            int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
            final String fNewValue = newValue;
            final long fDelta = delta;
            final int fTtl = ttl;
            final Map<String, String> fInitFields = initFields;
            final int fStatsTtl = statsTtl;

            List<Object> writeResults = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) throws DataAccessException {
                    // 初始化 Hash（如果需要）
                    if (needInit && fInitFields != null) {
                        for (Map.Entry<String, String> entry : fInitFields.entrySet()) {
                            operations.opsForHash().putIfAbsent(statsKey, entry.getKey(), entry.getValue());
                        }
                        operations.expire(statsKey, fStatsTtl, TimeUnit.SECONDS);
                    }
                    // 写入点赞状态
                    operations.opsForValue().set(likeKey, fNewValue, fTtl, TimeUnit.SECONDS);
                    // 更新计数
                    operations.opsForHash().increment(statsKey, "likeCount", fDelta);
                    // 标记脏数据
                    operations.opsForSet().add(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY, String.valueOf(pictureId));
                    // 读取最新计数
                    operations.opsForHash().get(statsKey, "likeCount");
                    return null;
                }
            });

            // 最终计数是 pipeline 最后一个命令的结果
            String likeCountStr = (String) writeResults.get(writeResults.size() - 1);
            int likeCount = likeCountStr != null ? Integer.parseInt(likeCountStr) : 0;

            // DB 操作 → 异步 MQ
            sendSocialActionMessage("like", liked ? "insert" : "delete", pictureId, userId, picture.getUserId());

            ToggleLikeVO result = new ToggleLikeVO();
            result.setLiked(liked);
            result.setLikeCount(Math.max(likeCount, 0));

            // 版本号递增，使旧缓存自然过期失效
            incrListVersion(RedisKeyConstants.LIST_LIKED_VERSION_KEY, userId);

            return result;
        }, "操作过于频繁，请稍后再试");
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

        // Pipeline 批量查询 Redis
        List<String> likeKeys = pictureIds.stream()
                .map(id -> String.format(RedisKeyConstants.SOCIAL_LIKE_KEY, userId, id))
                .collect(Collectors.toList());
        List<String> values = pipelineBatchGet(likeKeys);

        for (int i = 0; i < pictureIds.size(); i++) {
            String value = values.get(i);
            if (value != null) {
                result.put(pictureIds.get(i), "1".equals(value));
            } else {
                missedIds.add(pictureIds.get(i));
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
                int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(likeKey, liked ? "1" : "0", ttl, TimeUnit.SECONDS);
            }
        }

        return result;
    }

    /**
     * 收藏/取消收藏（toggle）
     * <p>
     * Redis 操作同步执行（用户即刻获得响应），DB 写入通过 RabbitMQ 异步处理。
     * 快速重复点击场景：Redis 分布式锁保证串行化，DB 唯一索引冲突由 Consumer 端静默忽略。
     * </p>
     */
    @Override
    public ToggleFavoriteVO toggleFavorite(Long pictureId, Long userId) {
        Picture picture = validPicturePublic(pictureId);

        String lockKey = String.format(RedisKeyConstants.SOCIAL_LOCK_KEY, "fav", userId, pictureId);
        return redisCacheUtil.executeWithLock(lockKey, () -> {
            String favKey = String.format(RedisKeyConstants.SOCIAL_FAV_KEY, userId, pictureId);
            String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);

            // 确保 Hash 已初始化
            initStatsHashIfNeeded(pictureId);

            boolean favorited;
            String existing = stringRedisTemplate.opsForValue().get(favKey);
            if ("1".equals(existing)) {
                // 已收藏 → 取消收藏，value 设为 "0"
                int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(favKey, "0", ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForHash().increment(statsKey, "favoriteCount", -1);
                // DB 删除 → 异步 MQ
                sendSocialActionMessage("favorite", "delete", pictureId, userId, picture.getUserId());
                favorited = false;
            } else {
                // 未收藏 → 收藏，value 设为 "1"
                int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(favKey, "1", ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForHash().increment(statsKey, "favoriteCount", 1);
                // DB 插入 + 通知 → 异步 MQ
                sendSocialActionMessage("favorite", "insert", pictureId, userId, picture.getUserId());
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

            // 版本号递增，使旧缓存自然过期失效
            incrListVersion(RedisKeyConstants.LIST_FAV_VERSION_KEY, userId);

            return result;
        }, "操作过于频繁，请稍后再试");
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

        // Pipeline 批量查询 Redis
        List<String> favKeys = pictureIds.stream()
                .map(id -> String.format(RedisKeyConstants.SOCIAL_FAV_KEY, userId, id))
                .collect(Collectors.toList());
        List<String> values = pipelineBatchGet(favKeys);

        for (int i = 0; i < pictureIds.size(); i++) {
            String value = values.get(i);
            if (value != null) {
                result.put(pictureIds.get(i), "1".equals(value));
            } else {
                missedIds.add(pictureIds.get(i));
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
                int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(favKey, favorited ? "1" : "0", ttl, TimeUnit.SECONDS);
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

        // Pipeline 批量 HGETALL
        List<String> statsKeys = pictureIds.stream()
                .map(id -> String.format(RedisKeyConstants.SOCIAL_STATS_KEY, id))
                .collect(Collectors.toList());
        List<Map<Object, Object>> entries = pipelineBatchHGetAll(statsKeys);

        for (int i = 0; i < pictureIds.size(); i++) {
            Map<Object, Object> entry = entries.get(i);
            if (entry != null && !entry.isEmpty()) {
                result.put(pictureIds.get(i), hashToStatsVO(entry));
            } else {
                missedIds.add(pictureIds.get(i));
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
                    // 写入 Redis Hash（使用 putIfAbsent 防止覆盖并发 INCR 的结果）
                    String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);
                    int like = stat.getLikeCount() != null ? stat.getLikeCount() : 0;
                    int fav = stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0;
                    int share = stat.getShareCount() != null ? stat.getShareCount() : 0;
                    int view = stat.getViewCount() != null ? stat.getViewCount() : 0;
                    int download = stat.getDownloadCount() != null ? stat.getDownloadCount() : 0;

                    stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "likeCount", String.valueOf(like));
                    stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "favoriteCount", String.valueOf(fav));
                    stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "shareCount", String.valueOf(share));
                    stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "viewCount", String.valueOf(view));
                    stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "downloadCount", String.valueOf(download));
                    int ttl = RedisKeyConstants.SOCIAL_STATS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATS_TTL_JITTER);
                    stringRedisTemplate.expire(statsKey, ttl, TimeUnit.SECONDS);

                    PictureStatisticsVO vo = new PictureStatisticsVO();
                    vo.setLikeCount(like);
                    vo.setFavoriteCount(fav);
                    vo.setShareCount(share);
                    vo.setViewCount(view);
                    vo.setDownloadCount(download);
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

    /**
     * 获取指定用户点赞图片列表
     * @param userId   目标用户 id
     * @param queryDTO 查询条件（分页、筛选、排序）
     * @return
     */
    @Override
    public Page<PictureBriefVO> getUserLikedPictures(Long userId, UserPictureQueryDTO queryDTO) {
        String md5 = buildPageMd5(queryDTO.getCurrent(), queryDTO.getPageSize());
        String version = getListVersion(RedisKeyConstants.LIST_LIKED_VERSION_KEY, userId);
        String cacheKey = String.format(RedisKeyConstants.LIST_LIKED_KEY, userId, version, md5);
        int ttl = 300 + RandomUtil.randomInt(0, 180);

        String json = redisCacheUtil.getWithLock(cacheKey, ttl, () -> {
            Page<PictureBriefVO> result = dbSocialService.getUserLikedPictures(userId, queryDTO);
            // 缓存前移除社交统计，只存图片基本信息
            stripSocialStats(result.getRecords());
            return JSONUtil.toJsonStr(result);
        });

        if (json == null) {
            return new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
        }
        Page<PictureBriefVO> page = deserializePictureBriefVOPage(json, queryDTO.getCurrent(), queryDTO.getPageSize());
        // 反序列化后通过 batchStatistics 实时填充社交数据
        fillSocialStats(page.getRecords());
        return page;
    }

    /**
     *  获取指定用户收藏图片列表
     * @param userId   目标用户 id
     * @param queryDTO 查询条件（分页、筛选、排序）
     * @return
     */
    @Override
    public Page<PictureBriefVO> getUserFavoritedPictures(Long userId, UserPictureQueryDTO queryDTO) {
        String md5 = buildPageMd5(queryDTO.getCurrent(), queryDTO.getPageSize());
        String version = getListVersion(RedisKeyConstants.LIST_FAV_VERSION_KEY, userId);
        String cacheKey = String.format(RedisKeyConstants.LIST_FAV_KEY, userId, version, md5);
        int ttl = 300 + RandomUtil.randomInt(0, 180);

        String json = redisCacheUtil.getWithLock(cacheKey, ttl, () -> {
            Page<PictureBriefVO> result = dbSocialService.getUserFavoritedPictures(userId, queryDTO);
            // 缓存前移除社交统计，只存图片基本信息
            stripSocialStats(result.getRecords());
            return JSONUtil.toJsonStr(result);
        });

        if (json == null) {
            return new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
        }
        Page<PictureBriefVO> page = deserializePictureBriefVOPage(json, queryDTO.getCurrent(), queryDTO.getPageSize());
        // 反序列化后通过 batchStatistics 实时填充社交数据
        fillSocialStats(page.getRecords());
        return page;
    }

    /**
     * 获取用户上传图片列表
     * @param userId   目标用户 id
     * @param queryDTO 查询条件（分页）
     * @return
     */
    // todo 这里的三个图片相关的方法应该移动到 Picture 模块下 ；三个方法流程一致 可抽象为模板方法
    @Override
    public Page<PictureBriefVO> getUserUploadedPictures(Long userId, UserPictureQueryDTO queryDTO) {
        String md5 = buildPageMd5(queryDTO.getCurrent(), queryDTO.getPageSize());
        String cacheKey = String.format(RedisKeyConstants.LIST_UPLOADED_KEY, userId, md5);
        int ttl = 300 + RandomUtil.randomInt(0, 180);

        String json = redisCacheUtil.getWithLock(cacheKey, ttl, () -> {
            Page<PictureBriefVO> result = dbSocialService.getUserUploadedPictures(userId, queryDTO);
            // 缓存前移除社交统计，只存图片基本信息
            stripSocialStats(result.getRecords());
            return JSONUtil.toJsonStr(result);
        });

        if (json == null) {
            return new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
        }
        Page<PictureBriefVO> page = deserializePictureBriefVOPage(json, queryDTO.getCurrent(), queryDTO.getPageSize());
        // 反序列化后通过 batchStatistics 实时填充社交数据
        fillSocialStats(page.getRecords());
        return page;
    }

    /**
     * 获取当前用户关注对象的图片列表（缓存版）
     * @param userId   当前登录用户 id
     * @param queryDTO 查询条件（分页、筛选、排序）
     * @return
     */
    @Override
    public Page<PictureBriefVO> getFollowingPictures(Long userId, UserPictureQueryDTO queryDTO) {
        String md5 = buildPageMd5(queryDTO.getCurrent(), queryDTO.getPageSize());
        String cacheKey = String.format(RedisKeyConstants.LIST_FOLLOWING_KEY, userId, md5);
        int ttl = 300 + RandomUtil.randomInt(0, 180);

        String json = redisCacheUtil.getWithLock(cacheKey, ttl, () -> {
            Page<PictureBriefVO> result = dbSocialService.getFollowingPictures(userId, queryDTO);
            // 缓存前移除社交统计，只存图片基本信息
            stripSocialStats(result.getRecords());
            return JSONUtil.toJsonStr(result);
        });

        if (json == null) {
            return new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
        }
        Page<PictureBriefVO> page = deserializePictureBriefVOPage(json, queryDTO.getCurrent(), queryDTO.getPageSize());
        // 反序列化后通过 batchStatistics 实时填充社交数据
        fillSocialStats(page.getRecords());
        return page;
    }

    // ==================== 私有方法 ====================

    private String buildPageMd5(int current, int pageSize) {
        String raw = "cur=" + current + "|ps=" + pageSize;
        return DigestUtils.md5DigestAsHex(raw.getBytes());
    }

    /**
     * 递增列表缓存版本号（O(1) 操作，替代 deleteByPattern 的 SCAN 开销）
     * 版本号 key 不存在时 INCR 自动从 0 开始递增到 1
     */
    private void incrListVersion(String versionKeyTemplate, Long userId) {
        try {
            String versionKey = String.format(versionKeyTemplate, userId);
            stringRedisTemplate.opsForValue().increment(versionKey);
            // 设置较长 TTL 防止永久残留（30 天）
            stringRedisTemplate.expire(versionKey, RedisKeyConstants.LIST_VERSION_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("递增列表缓存版本号失败，降级处理 | userId={}", userId, e);
        }
    }

    /**
     * 获取列表缓存版本号
     * 版本号不存在时默认返回 "0"
     */
    private String getListVersion(String versionKeyTemplate, Long userId) {
        try {
            String versionKey = String.format(versionKeyTemplate, userId);
            String version = stringRedisTemplate.opsForValue().get(versionKey);
            return version != null ? version : "0";
        } catch (Exception e) {
            log.warn("读取列表缓存版本号失败，降级为默认版本 | userId={}", userId, e);
            return "0";
        }
    }

    private Page<PictureBriefVO> deserializePictureBriefVOPage(String json, int current, int pageSize) {
        cn.hutool.json.JSONObject jsonObj = JSONUtil.parseObj(json);
        Page<PictureBriefVO> page = new Page<>(
                jsonObj.getInt("current", current),
                jsonObj.getInt("size", pageSize),
                jsonObj.getLong("total", 0L)
        );
        List<PictureBriefVO> records = jsonObj.getJSONArray("records")
                .stream()
                .map(obj -> ((cn.hutool.json.JSONObject) obj).toBean(PictureBriefVO.class))
                .collect(Collectors.toList());
        page.setRecords(records);
        return page;
    }

    /**
     * 校验图片是否为公共图库图片（带 Redis 缓存）
     * <p>
     * 缓存策略：pic:public:{pictureId} → Picture JSON
     * - 正常值：Picture 对象的 JSON 字符串
     * - 特殊标记 "DELETED"：图片不存在，直接抛异常
     * - 特殊标记 "PRIVATE"：私有空间图片，直接抛异常
     * - cache miss → 查 DB → 回填缓存
     * - Redis 异常时降级到 DB 查询，不影响核心功能
     * </p>
     */
    private Picture validPicturePublic(Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片 id 不合法");

        // 尝试从 Redis 缓存读取
        String cacheKey = String.format(RedisKeyConstants.PICTURE_PUBLIC_KEY, pictureId);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                // 特殊标记：图片已删除
                if ("DELETED".equals(cached)) {
                    ThrowUtils.throwIf(true, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
                }
                // 特殊标记：私有空间图片
                if ("PRIVATE".equals(cached)) {
                    ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR, "仅公共图库支持社交功能");
                }
                // 正常缓存命中，反序列化 Picture 对象
                return JSONUtil.toBean(cached, Picture.class);
            }
        } catch (Exception e) {
            // ThrowUtils.throwIf 抛出的业务异常需要继续向上传播
            if (e instanceof org.example.shared.exception.BusinessException) {
                throw (org.example.shared.exception.BusinessException) e;
            }
            // Redis 异常，降级到 DB 查询
            log.warn("Redis 读取图片公开状态缓存失败，降级到 DB 查询 | pictureId={}", pictureId, e);
        }

        // 缓存未命中或 Redis 异常降级 → 查 DB
        Picture picture = pictureMapper.selectById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(picture.getSpaceId() != null, ErrorCode.NO_AUTH_ERROR, "仅公共图库支持社交功能");

        // 回填缓存（try-catch 不影响主流程）
        try {
            String json = JSONUtil.toJsonStr(picture);
            int ttl = RedisKeyConstants.PICTURE_PUBLIC_TTL_BASE
                    + RandomUtil.randomInt(0, RedisKeyConstants.PICTURE_PUBLIC_TTL_JITTER);
            stringRedisTemplate.opsForValue().set(cacheKey, json, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 回填图片公开状态缓存失败 | pictureId={}", pictureId, e);
        }

        return picture;
    }

    /**
     * 发送社交操作消息到 RabbitMQ（异步处理 DB 写入 + 通知发布）
     *
     * @param actionType     操作类型：like / favorite
     * @param operation      操作行为：insert / delete
     * @param pictureId      图片ID
     * @param userId         操作用户ID
     * @param pictureOwnerId 图片作者ID
     */
    private void sendSocialActionMessage(String actionType, String operation,
                                         Long pictureId, Long userId, Long pictureOwnerId) {
        try {
            SocialActionMessage message = new SocialActionMessage();
            message.setActionType(actionType);
            message.setOperation(operation);
            message.setPictureId(pictureId);
            message.setUserId(userId);
            message.setPictureOwnerId(pictureOwnerId);

            rabbitTemplate.convertAndSend(
                    SocialActionMQConfig.SOCIAL_ACTION_EXCHANGE,
                    SocialActionMQConfig.SOCIAL_ACTION_ROUTING_KEY,
                    cn.hutool.json.JSONUtil.toJsonStr(message)
            );

            log.debug("社交操作消息已发送 | actionType={}, operation={}, userId={}, pictureId={}",
                    actionType, operation, userId, pictureId);
        } catch (Exception e) {
            log.error("社交操作消息发送失败 | actionType={}, operation={}, userId={}, pictureId={}",
                    actionType, operation, userId, pictureId, e);
        }
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
     * 如果 Redis Hash 不存在，从 DB 初始化（使用 putIfAbsent 防止覆盖并发 INCR）
     */
    private void initStatsHashIfNeeded(Long pictureId) {
        String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);
        Boolean exists = stringRedisTemplate.hasKey(statsKey);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        PictureStatistics stat = pictureStatisticsMapper.selectById(pictureId);
        int like = 0, fav = 0, share = 0, view = 0, download = 0;
        if (stat != null) {
            like = stat.getLikeCount() != null ? stat.getLikeCount() : 0;
            fav = stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0;
            share = stat.getShareCount() != null ? stat.getShareCount() : 0;
            view = stat.getViewCount() != null ? stat.getViewCount() : 0;
            download = stat.getDownloadCount() != null ? stat.getDownloadCount() : 0;
        }

        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "likeCount", String.valueOf(like));
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "favoriteCount", String.valueOf(fav));
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "shareCount", String.valueOf(share));
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "viewCount", String.valueOf(view));
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "downloadCount", String.valueOf(download));
        int ttl = RedisKeyConstants.SOCIAL_STATS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATS_TTL_JITTER);
        stringRedisTemplate.expire(statsKey, ttl, TimeUnit.SECONDS);
    }

    /**
     * 初始化 Redis Hash 为零值（使用 putIfAbsent 防止覆盖）
     */
    private void initStatsHash(Long pictureId, int like, int fav, int share, int view, int download) {
        String statsKey = String.format(RedisKeyConstants.SOCIAL_STATS_KEY, pictureId);
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "likeCount", String.valueOf(like));
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "favoriteCount", String.valueOf(fav));
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "shareCount", String.valueOf(share));
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "viewCount", String.valueOf(view));
        stringRedisTemplate.opsForHash().putIfAbsent(statsKey, "downloadCount", String.valueOf(download));
        int ttl = RedisKeyConstants.SOCIAL_STATS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATS_TTL_JITTER);
        stringRedisTemplate.expire(statsKey, ttl, TimeUnit.SECONDS);
    }

    /**
     * Pipeline 批量 GET
     */
    private List<String> pipelineBatchGet(List<String> keys) {
        List<Object> results = stringRedisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    StringRedisConnection stringConn = (StringRedisConnection) connection;
                    for (String key : keys) {
                        stringConn.get(key);
                    }
                    return null;
                }
        );
        return results.stream().map(obj -> obj != null ? obj.toString() : null)
                .collect(Collectors.toList());
    }

    /**
     * Pipeline 批量 HGETALL
     */
    @SuppressWarnings("unchecked")
    private List<Map<Object, Object>> pipelineBatchHGetAll(List<String> keys) {
        List<Object> results = stringRedisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    StringRedisConnection stringConn = (StringRedisConnection) connection;
                    for (String key : keys) {
                        stringConn.hGetAll(key);
                    }
                    return null;
                }
        );
        return (List<Map<Object, Object>>) (List<?>) results;
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
        if (val == null) {
            return 0;
        }
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

    /**
     * 缓存前移除社交统计（缓存只存图片基本信息）
     */
    private void stripSocialStats(List<PictureBriefVO> records) {
        if (records == null) {
            return;
        }
        for (PictureBriefVO vo : records) {
            vo.setLikeCount(null);
            vo.setFavoriteCount(null);
            vo.setViewCount(null);
            vo.setDownloadCount(null);
        }
    }

    /**
     * 反序列化后通过 batchStatistics 实时填充社交统计
     */
    private void fillSocialStats(List<PictureBriefVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> pictureIds = records.stream()
                .map(PictureBriefVO::getId)
                .collect(Collectors.toList());
        Map<Long, PictureStatisticsVO> statsMap = batchStatistics(pictureIds);
        for (PictureBriefVO vo : records) {
            PictureStatisticsVO stats = statsMap.getOrDefault(vo.getId(), defaultStats());
            vo.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
            vo.setFavoriteCount(stats.getFavoriteCount() != null ? stats.getFavoriteCount() : 0);
            vo.setViewCount(stats.getViewCount() != null ? stats.getViewCount() : 0);
            vo.setDownloadCount(stats.getDownloadCount() != null ? stats.getDownloadCount() : 0);
        }
    }
}
