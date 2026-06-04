package org.example.server.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.RedisCacheUtil;
import org.example.pojo.dto.social.UserPictureQueryDTO;
import org.example.pojo.entity.Picture;
import org.example.pojo.entity.PictureFavorite;
import org.example.pojo.entity.PictureLike;
import org.example.pojo.entity.PictureStatistics;
import org.example.pojo.entity.User;
import org.example.pojo.vo.PictureBriefVO;
import org.example.pojo.vo.PictureStatisticsVO;
import org.example.pojo.vo.ToggleFavoriteVO;
import org.example.pojo.vo.ToggleLikeVO;
import org.example.common.constants.RedisKeyConstants;
import org.example.server.mapper.PictureFavoriteMapper;
import org.example.server.mapper.PictureLikeMapper;
import org.example.server.mapper.PictureMapper;
import org.example.server.mapper.PictureStatisticsMapper;
import org.example.server.mapper.UserMapper;
import org.example.server.service.SocialService;
import org.example.server.service.event.NotificationEvent;
import org.example.common.enums.NotificationTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
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
// todo 添加关注功能 在用户主页面 可以看到该用户关注了多少人 和该用户拥有多少粉丝 相关列表在 UserService中实现 分页实现
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

    @Resource
    private UserMapper userMapper;

    @Resource
    private DefaultRedisScript<Long> releaseLockScript;

    @Resource(name = "dbSocialService")
    private SocialService dbSocialService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 分布式锁超时时间（秒）
     */
    private static final int LOCK_TIMEOUT_SECONDS = 10;

    /**
     * TODO: 优化方向 — 当前 DB 写操作在分布式锁内执行（锁 TTL 10s）。
     *       如果未来 DB 写入延迟成为瓶颈，可考虑将 DB 操作移到锁外：
     *       先完成 Redis 操作 + 释放锁，再同步写 DB（需处理 DB 失败时的回补逻辑）。
     *       当前业务量级下单行 INSERT/DELETE 正常 < 100ms，暂不需要优化。
     *       DB 写入延迟成为瓶颈后，采用 rabbitMQ 削峰
     *
     */
    @Override
    public ToggleLikeVO toggleLike(Long pictureId, Long userId) {
        Picture picture = validPicturePublic(pictureId);

        String lockKey = String.format(RedisKeyConstants.SOCIAL_LOCK_KEY, "like", userId, pictureId);
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
                int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(likeKey, "0", ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForHash().increment(statsKey, "likeCount", -1);
                // DB 删除
                QueryWrapper<PictureLike> qw = new QueryWrapper<>();
                qw.eq("pictureId", pictureId).eq("userId", userId);
                pictureLikeMapper.delete(qw);
                liked = false;
            } else {
                // 未点赞 → 点赞，value 设为 "1"
                int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(likeKey, "1", ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForHash().increment(statsKey, "likeCount", 1);
                // DB 插入
                PictureLike pictureLike = new PictureLike();
                pictureLike.setPictureId(pictureId);
                pictureLike.setUserId(userId);
                pictureLikeMapper.insert(pictureLike);
                liked = true;

                // 发布点赞通知事件
                publishNotification(picture, userId, NotificationTypeEnum.LIKE, "赞了你的图片");
            }

            // 标记脏数据
            stringRedisTemplate.opsForSet().add(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY, String.valueOf(pictureId));

            // 读取最新计数
            String likeCountStr = (String) stringRedisTemplate.opsForHash().get(statsKey, "likeCount");
            int likeCount = likeCountStr != null ? Integer.parseInt(likeCountStr) : 0;

            ToggleLikeVO result = new ToggleLikeVO();
            result.setLiked(liked);
            result.setLikeCount(Math.max(likeCount, 0));

            // 失效用户点赞列表缓存
            redisCacheUtil.deleteByPattern(String.format("list:liked:%d:*", userId));

            return result;
        } finally {
            stringRedisTemplate.execute(releaseLockScript, Collections.singletonList(lockKey), lockValue);
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
     * TODO: 同 toggleLike — DB 写操作在锁内，当前可接受，未来按需优化
     */
    @Override
    public ToggleFavoriteVO toggleFavorite(Long pictureId, Long userId) {
        Picture picture = validPicturePublic(pictureId);

        String lockKey = String.format(RedisKeyConstants.SOCIAL_LOCK_KEY, "fav", userId, pictureId);
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
                int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(favKey, "0", ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForHash().increment(statsKey, "favoriteCount", -1);
                // DB 删除
                QueryWrapper<PictureFavorite> qw = new QueryWrapper<>();
                qw.eq("pictureId", pictureId).eq("userId", userId);
                pictureFavoriteMapper.delete(qw);
                favorited = false;
            } else {
                // 未收藏 → 收藏，value 设为 "1"
                int ttl = RedisKeyConstants.SOCIAL_STATUS_TTL_BASE + RandomUtil.randomInt(0, RedisKeyConstants.SOCIAL_STATUS_TTL_JITTER);
                stringRedisTemplate.opsForValue().set(favKey, "1", ttl, TimeUnit.SECONDS);
                stringRedisTemplate.opsForHash().increment(statsKey, "favoriteCount", 1);
                // DB 插入
                PictureFavorite pictureFavorite = new PictureFavorite();
                pictureFavorite.setPictureId(pictureId);
                pictureFavorite.setUserId(userId);
                pictureFavoriteMapper.insert(pictureFavorite);
                favorited = true;

                // 发布收藏通知事件
                publishNotification(picture, userId, NotificationTypeEnum.FAVORITE, "收藏了你的图片");
            }

            // todo 在点赞收藏阶段 用户重复点击 导致违背数据库唯一索引 需要捕获异常并提示前端点击过快
            // 标记脏数据
            stringRedisTemplate.opsForSet().add(RedisKeyConstants.SOCIAL_STATS_DIRTY_KEY, String.valueOf(pictureId));

            // 读取最新计数
            String favCountStr = (String) stringRedisTemplate.opsForHash().get(statsKey, "favoriteCount");
            int favoriteCount = favCountStr != null ? Integer.parseInt(favCountStr) : 0;

            ToggleFavoriteVO result = new ToggleFavoriteVO();
            result.setFavorited(favorited);
            result.setFavoriteCount(Math.max(favoriteCount, 0));

            // 失效用户收藏列表缓存
            redisCacheUtil.deleteByPattern(String.format("list:fav:%d:*", userId));

            return result;
        } finally {
            stringRedisTemplate.execute(releaseLockScript, Collections.singletonList(lockKey), lockValue);
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
        String cacheKey = String.format(RedisKeyConstants.LIST_LIKED_KEY, userId, md5);
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
        String cacheKey = String.format(RedisKeyConstants.LIST_FAV_KEY, userId, md5);
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

    // ==================== 私有方法 ====================

    private String buildPageMd5(int current, int pageSize) {
        String raw = "cur=" + current + "|ps=" + pageSize;
        return DigestUtils.md5DigestAsHex(raw.getBytes());
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

    private Picture validPicturePublic(Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片 id 不合法");
        Picture picture = pictureMapper.selectById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(picture.getSpaceId() != null, ErrorCode.NO_AUTH_ERROR, "仅公共图库支持社交功能");
        return picture;
    }

    /**
     * 发布通知事件（异步处理）
     */
    // todo 如果要考虑高并发场景下的点赞收藏模式下，需要加入 rabbitMQ 进行削峰即可
    private void publishNotification(Picture picture, Long senderId, NotificationTypeEnum type, String action) {
        try {
            // 获取触发者信息
            User sender = userMapper.selectById(senderId);
            String senderName = sender != null ? sender.getUserName() : "匿名用户";
            String senderAvatar = sender != null ? sender.getUserAvatar() : null;

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    picture.getUserId(),  // 接收者 = 图片作者
                    senderId,
                    senderName,
                    senderAvatar,
                    type,
                    action,
                    null,
                    picture.getId(),
                    "/picture/" + picture.getId()
            ));
        } catch (Exception e) {
            log.warn("发布通知事件失败：pictureId={}, senderId={}, type={}", picture.getId(), senderId, type, e);
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
