package org.example.picture.social.application.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.constants.RedisKeyConstants;
import org.example.identity.api.UserContext;
import org.example.shared.util.RedisCacheUtil;
import org.example.picture.social.interfaces.dto.RecommendQueryDTO;
import org.example.picture.core.domain.model.Category;
import org.example.picture.core.domain.model.Picture;
import org.example.picture.core.domain.model.PictureWithStats;
import org.example.identity.api.model.User;
import org.example.picture.core.interfaces.vo.PictureSocialVO;
import org.example.picture.core.interfaces.vo.PictureStatisticsVO;
import org.example.picture.core.interfaces.vo.PictureVO;
import org.example.picture.social.interfaces.vo.RecommendVO;
import org.example.identity.interfaces.vo.UserVO;
import org.example.picture.core.infrastructure.persistence.PictureMapper;
import org.example.picture.core.application.CategoryService;
import org.example.picture.social.application.RecommendService;
import org.example.picture.social.application.SocialService;
import org.example.identity.application.UserService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推荐服务实现
 * <p>
 * 热度分数生命周期：
 * 社交行为 → Redis Hash (social:stats:dirty)
 * → PictureStatisticsSyncScheduled 同步 DB + 写入 rec:hot:dirty
 * → HotScoreSyncScheduled 重算 hotScore → 持久化 picture.hotScore + 写入 Redis ZSET
 * → 缓存过期 / Redis 重启 → RecommendPreheatRunner 从 DB hotScore 恢复 ZSET
 * <p>
 * 查询链路：ZSET 仅用于排序，图片数据 + 社交信息复用标准查询链路
 *
 * @author Zou
 */
@Slf4j
@Service
public class RecommendServiceImpl implements RecommendService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource
    private PictureMapper pictureMapper;

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource
    private CategoryService categoryService;

    @Resource(name = "cachedSocialService")
    private SocialService cachedSocialService;

    /**
     * 热度评分权重
     */
    private static final double W_LIKE = 3.0;
    private static final double W_FAV = 5.0;
    private static final double W_SHARE = 2.0;
    private static final double W_VIEW = 0.5;
    private static final double W_DOWNLOAD = 4.0;

    /**
     * Newton 冷却系数（半衰期 ~4.3 天）
     */
    private static final double COOLING_FACTOR = 0.008;

    /**
     * 热度 ZSET 最大保留数（超出按分数裁剪）
     */
    private static final int ZSET_MAX_SIZE = 10000;

    @Override
    public RecommendVO recommend(RecommendQueryDTO queryDTO, Long userId) {
        String scene = queryDTO.getScene();
        if (StrUtil.isBlank(scene)) {
            scene = "homepage";
        }

        if ("guess".equals(scene) && userId == null) {
            scene = "homepage";
        }

        switch (scene) {
            case "homepage":
                return recommendHomepage(queryDTO);
            case "guess":
                return recommendGuess(queryDTO, userId);
            case "similar":
                return recommendSimilar(queryDTO);
            default:
                return recommendHomepage(queryDTO);
        }
    }

    // ==================== 场景推荐 ====================

    /**
     * 首页热度推荐（支持分类筛选 + 排除列表）
     * <p>
     * 查询链路：categoryId 有值 → 分类 ZSET → 分类 DB 降级
     * categoryId 无值 → 全局 ZSET → 全局 DB 降级
     * <p>
     * ZSET 提供排序 ID → 查询完整 PictureVO（含 userVO）→ 缓存 Page JSON → 实时填充社交数据
     * 与 {@code CachedPictureServiceImpl.queryPictureListUser} 模式一致
     */
    private RecommendVO recommendHomepage(RecommendQueryDTO queryDTO) {
        int current = queryDTO.getCurrent();
        int pageSize = queryDTO.getPageSize();
        int offset = (current - 1) * pageSize;
        Long categoryId = queryDTO.getCategoryId();
        List<Long> excludeIds = queryDTO.getExcludeIds();

        // 确定使用的 ZSET key 和缓存分类 ID（0 = 全部分类）
        final String zsetKey;
        final long cacheCategoryId;
        if (categoryId != null && categoryId > 0) {
            zsetKey = String.format(RedisKeyConstants.REC_HOT_CAT_ZSET_KEY, categoryId);
            cacheCategoryId = categoryId;
        } else {
            zsetKey = RedisKeyConstants.REC_HOT_ZSET_KEY;
            cacheCategoryId = 0L;
        }

        // 检查 ZSET 是否存在
        final Long zsetSize = stringRedisTemplate.opsForZSet().size(zsetKey);
        if (zsetSize == null || zsetSize == 0) {
            return recommendHomepageFromDb(queryDTO);
        }

        // 构建缓存 key（包含分类 ID）
        String cacheKey = String.format(RedisKeyConstants.REC_RESULT_KEY,
                "homepage", cacheCategoryId, current, pageSize);

        int ttl = RedisKeyConstants.REC_RESULT_TTL_BASE
                + ThreadLocalRandom.current().nextInt(RedisKeyConstants.REC_RESULT_TTL_JITTER + 1);

        // 有排除列表时跳过缓存（排除列表组合多且变化频繁，不适合缓存）
        final boolean hasExcludes = excludeIds != null && !excludeIds.isEmpty();

        String json;
        if (hasExcludes) {
            json = queryHomepageFromZset(zsetKey, offset, pageSize, zsetSize, excludeIds);
        } else {
            // 缓存存储 Page<PictureVO> 的 JSON（不含 socialInfo）
            json = redisCacheUtil.getWithLock(cacheKey, ttl, () ->
                    queryHomepageFromZset(zsetKey, offset, pageSize, zsetSize, null));
        }

        // 反序列化
        List<PictureVO> pictures = parsePictureVORecords(json);
        long total = parseTotal(json);

        // 实时填充社交数据（与 queryPictureListUser 一致，不缓存）
        fillSocialData(pictures);

        boolean hasMore = total > (offset + pageSize);

        RecommendVO vo = new RecommendVO();
        vo.setPictures(pictures);
        vo.setScene("homepage");
        vo.setReason("热度推荐");
        vo.setHasMore(hasMore);
        return vo;
    }

    /**
     * 从 ZSET 查询一页图片 ID，构建 PictureVO 列表并序列化为 JSON
     *
     * @param zsetKey    ZSET key
     * @param offset     分页偏移
     * @param pageSize   每页大小
     * @param zsetSize   ZSET 总大小
     * @param excludeIds 需要排除的 ID 列表（可为 null）
     * @return 序列化后的 JSON 字符串
     */
    private String queryHomepageFromZset(String zsetKey, int offset, int pageSize,
                                          long zsetSize, List<Long> excludeIds) {
        final boolean hasExcludes = excludeIds != null && !excludeIds.isEmpty();

        // 有排除列表时多取一些，避免过滤后数量不足
        int fetchEnd;
        if (hasExcludes) {
            fetchEnd = (int) Math.min(offset + pageSize + excludeIds.size(), zsetSize) - 1;
        } else {
            fetchEnd = offset + pageSize - 1;
        }

        Set<String> idSet = stringRedisTemplate.opsForZSet()
                .reverseRange(zsetKey, offset, fetchEnd);

        if (idSet == null || idSet.isEmpty()) {
            return "{\"records\":[]}";
        }

        List<Long> orderedIds = idSet.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 过滤排除的 ID
        if (hasExcludes) {
            orderedIds.removeIf(excludeIds::contains);
            // 截取 pageSize 条
            if (orderedIds.size() > pageSize) {
                orderedIds = orderedIds.subList(0, pageSize);
            }
        }

        List<PictureVO> voList = buildPictureVOList(orderedIds);

        // 计算有效 total（去除排除项）
        long effectiveTotal = hasExcludes
                ? Math.max(0, zsetSize - excludeIds.size())
                : zsetSize;

        JSONObject result = new JSONObject();
        result.set("records", voList);
        result.set("total", effectiveTotal);
        return result.toString();
    }

    /**
     * DB 降级查询：当 ZSET 为空时，按 hotScore 降序查询 DB
     * 支持分类筛选
     */
    private RecommendVO recommendHomepageFromDb(RecommendQueryDTO queryDTO) {
        int pageSize = queryDTO.getPageSize();
        int offset = (queryDTO.getCurrent() - 1) * pageSize;
        Long categoryId = queryDTO.getCategoryId();
        List<Long> excludeIds = queryDTO.getExcludeIds();

        // 按分类或全局查询 DB hotScore 排序的图片
        List<PictureWithStats> statsList;
        if (categoryId != null && categoryId > 0) {
            statsList = pictureMapper.selectHotPicturesByCategory(
                    categoryId, offset, pageSize + 1);
        } else {
            statsList = pictureMapper.selectHotPictures(offset, pageSize + 1);
        }

        if (statsList == null || statsList.isEmpty()) {
            RecommendVO vo = new RecommendVO();
            vo.setPictures(Collections.emptyList());
            vo.setScene("homepage");
            vo.setReason("热度推荐");
            vo.setHasMore(false);
            return vo;
        }

        boolean hasMore = statsList.size() > pageSize;
        if (hasMore) {
            statsList = statsList.subList(0, pageSize);
        }

        List<Long> orderedIds = statsList.stream()
                .map(PictureWithStats::getId)
                .collect(Collectors.toList());

        // 过滤排除的 ID
        if (excludeIds != null && !excludeIds.isEmpty()) {
            orderedIds.removeIf(excludeIds::contains);
        }

        List<PictureVO> pictures = buildPictureVOList(orderedIds);
        fillSocialData(pictures);

        RecommendVO vo = new RecommendVO();
        vo.setPictures(pictures);
        vo.setScene("homepage");
        vo.setReason("热度推荐");
        vo.setHasMore(hasMore);
        return vo;
    }

    /**
     * 猜你喜欢：Phase 1 降级为热度推荐
     */
    private RecommendVO recommendGuess(RecommendQueryDTO queryDTO, Long userId) {
        RecommendVO vo = recommendHomepage(queryDTO);
        vo.setScene("guess");
        vo.setReason("猜你喜欢");
        return vo;
    }

    /**
     * 相似图片：Phase 3 实现，当前降级为热度推荐
     */
    private RecommendVO recommendSimilar(RecommendQueryDTO queryDTO) {
        RecommendVO vo = recommendHomepage(queryDTO);
        vo.setScene("similar");
        vo.setReason("相似图片");
        return vo;
    }

    // ==================== 热度计算 ====================

    @Override
    public double calculateHotScore(PictureWithStats stats) {
        if (stats == null) {
            return 0.0;
        }

        int like = nullToZero(stats.getLikeCount());
        int fav = nullToZero(stats.getFavoriteCount());
        int share = nullToZero(stats.getShareCount());
        int view = nullToZero(stats.getViewCount());
        int download = nullToZero(stats.getDownloadCount());

        double numerator = W_LIKE * Math.log1p(like)
                + W_FAV * Math.log1p(fav)
                + W_SHARE * Math.log1p(share)
                + W_VIEW * Math.log1p(view / 100.0)
                + W_DOWNLOAD * Math.log1p(download);

        long hoursSinceCreate = 0;
        if (stats.getCreateTime() != null) {
            hoursSinceCreate = Math.max(0,
                    (System.currentTimeMillis() - stats.getCreateTime().getTime()) / 3600000L);
        }
        double denominator = 1.0 + COOLING_FACTOR * hoursSinceCreate;

        return numerator / denominator;
    }

    // ==================== 全量重算（Pipeline + 持久化） ====================

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public int rebuildHotScores() {
        log.info("开始全量重算热度分数...");
        long startTime = System.currentTimeMillis();

        List<Long> allIds = pictureMapper.selectAllPublicPictureIds();
        if (allIds == null || allIds.isEmpty()) {
            log.info("无公开图片，跳过热度重算");
            return 0;
        }

        log.info("待重算图片数量: {}", allIds.size());

        int batchSize = 500;
        int totalProcessed = 0;
        Map<Long, Double> scoreMap = new LinkedHashMap<>();

        for (int i = 0; i < allIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allIds.size());
            List<Long> batchIds = allIds.subList(i, end);

            List<PictureWithStats> statsList = pictureMapper.selectWithStats(batchIds);
            if (statsList == null || statsList.isEmpty()) {
                continue;
            }

            for (PictureWithStats stats : statsList) {
                double score = calculateHotScore(stats);
                scoreMap.put(stats.getId(), score);
            }

            // Pipeline 批量写入 ZSET（减少 Redis 网络往返）
            stringRedisTemplate.executePipelined(
                    new SessionCallback<Object>() {
                        @Override
                        public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                            for (PictureWithStats stats : statsList) {
                                double score = scoreMap.get(stats.getId());
                                operations.opsForZSet().add(
                                        (K) RedisKeyConstants.REC_HOT_ZSET_KEY,
                                        (V) String.valueOf(stats.getId()),
                                        score
                                );
                                if (stats.getCategoryId() != null) {
                                    String catKey = String.format(RedisKeyConstants.REC_HOT_CAT_ZSET_KEY, stats.getCategoryId());
                                    operations.opsForZSet().add((K) catKey, (V) String.valueOf(stats.getId()), score);
                                }
                            }
                            return null;
                        }
                    }
            );

            totalProcessed += statsList.size();
        }

        // 裁剪 ZSET
        Long totalSize = stringRedisTemplate.opsForZSet().size(RedisKeyConstants.REC_HOT_ZSET_KEY);
        if (totalSize != null && totalSize > ZSET_MAX_SIZE) {
            stringRedisTemplate.opsForZSet()
                    .removeRange(RedisKeyConstants.REC_HOT_ZSET_KEY, 0, totalSize - ZSET_MAX_SIZE - 1);
        }

        stringRedisTemplate.expire(RedisKeyConstants.REC_HOT_ZSET_KEY,
                RedisKeyConstants.REC_HOT_ZSET_TTL, TimeUnit.SECONDS);

        // 持久化 hotScore 到 DB
        persistHotScores(scoreMap);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("热度重算完成，处理 {} 张图片，耗时 {} ms", totalProcessed, elapsed);

        return totalProcessed;
    }

    private void persistHotScores(Map<Long, Double> scoreMap) {
        if (scoreMap.isEmpty()) {
            return;
        }
        List<Map<String, Object>> batchList = new ArrayList<>(scoreMap.size());
        for (Map.Entry<Long, Double> entry : scoreMap.entrySet()) {
            Map<String, Object> item = new HashMap<>(2);
            item.put("id", entry.getKey());
            item.put("score", entry.getValue());
            batchList.add(item);
        }
        pictureMapper.batchUpdateHotScore(batchList);
        log.info("已持久化 {} 张图片的 hotScore 到 DB", batchList.size());
    }

    // ==================== 缓存预热 ====================

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public int preheatFromDb() {
        Long zsetSize = stringRedisTemplate.opsForZSet().size(RedisKeyConstants.REC_HOT_ZSET_KEY);
        if (zsetSize != null && zsetSize > 0) {
            log.info("热度 ZSET 已有 {} 条数据，跳过预热", zsetSize);
            return 0;
        }

        log.info("热度 ZSET 为空，从 DB 加载 hotScore 预热缓存...");
        long startTime = System.currentTimeMillis();

        List<PictureWithStats> statsList = pictureMapper.selectHotPictures(0, ZSET_MAX_SIZE);
        if (statsList == null || statsList.isEmpty()) {
            log.info("DB 中无热度数据，跳过预热");
            return 0;
        }

        int[] count = {0};
        stringRedisTemplate.executePipelined(
                new SessionCallback<Object>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        for (PictureWithStats stats : statsList) {
                            double score = calculateHotScore(stats);
                            operations.opsForZSet().add(
                                    (K) RedisKeyConstants.REC_HOT_ZSET_KEY,
                                    (V) String.valueOf(stats.getId()),
                                    score
                            );
                            count[0]++;
                        }
                        return null;
                    }
                }
        );

        stringRedisTemplate.expire(RedisKeyConstants.REC_HOT_ZSET_KEY,
                RedisKeyConstants.REC_HOT_ZSET_TTL, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("缓存预热完成，加载 {} 张图片到 ZSET，耗时 {} ms", count[0], elapsed);

        return count[0];
    }

    // ==================== PictureVO 构建（复用 doQueryPictureListUser 模式） ====================

    /**
     * 按 ZSET/Db 排序的 ID 列表，批量构建 PictureVO（含 userVO + categoryName）
     * 复用 {@code CachedPictureServiceImpl.doQueryPictureListUser} 的转换模式
     */
    private List<PictureVO> buildPictureVOList(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询 Picture 实体
        List<Picture> pictures = pictureMapper.selectBatchIds(orderedIds);
        if (pictures == null || pictures.isEmpty()) {
            return Collections.emptyList();
        }

        // ID → Picture 映射
        Map<Long, Picture> pictureMap = pictures.stream()
                .collect(Collectors.toMap(Picture::getId, p -> p, (a, b) -> a));

        // 批量查用户
        Set<Long> userIds = pictures.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 批量查分类
        Set<Long> categoryIds = pictures.stream()
                .map(Picture::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryService.listByIds(categoryIds)
                        .stream()
                        .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        // 按 ZSET 顺序构建 PictureVO 列表
        List<PictureVO> result = new ArrayList<>(orderedIds.size());
        for (Long id : orderedIds) {
            Picture picture = pictureMap.get(id);
            if (picture == null) {
                continue;
            }

            PictureVO vo = PictureVO.objToVO(picture);

            // 填充用户信息
            User user = userMap.get(picture.getUserId());
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtil.copyProperties(user, userVO);
                vo.setUserVO(userVO);
            }

            // 填充分类名称
            if (picture.getCategoryId() != null && categoryMap.containsKey(picture.getCategoryId())) {
                vo.setCategoryName(categoryMap.get(picture.getCategoryId()).getName());
            }

            result.add(vo);
        }

        return result;
    }

    // ==================== 社交数据填充（与 CachedPictureServiceImpl.fillSocialData 一致） ====================

    /**
     * 批量填充社交数据（仅公共图库图片）
     * 实时从缓存/DB 获取，不缓存到推荐缓存中
     */
    private void fillSocialData(List<PictureVO> pictureVOList) {
        if (pictureVOList == null || pictureVOList.isEmpty()) {
            return;
        }

        List<Long> publicIds = pictureVOList.stream()
                .filter(vo -> vo.getSpaceId() == null)
                .map(PictureVO::getId)
                .collect(Collectors.toList());
        if (publicIds.isEmpty()) {
            return;
        }

        User currentUser = UserContext.get();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        Map<Long, PictureStatisticsVO> statsMap = cachedSocialService.batchStatistics(publicIds);
        Map<Long, Boolean> likeStatusMap = currentUserId != null
                ? cachedSocialService.batchLikeStatus(publicIds, currentUserId)
                : Collections.emptyMap();
        Map<Long, Boolean> favStatusMap = currentUserId != null
                ? cachedSocialService.batchFavoriteStatus(publicIds, currentUserId)
                : Collections.emptyMap();

        for (PictureVO vo : pictureVOList) {
            if (vo.getSpaceId() != null) {
                continue;
            }
            Long picId = vo.getId();
            PictureStatisticsVO stats = statsMap.getOrDefault(picId, new PictureStatisticsVO());
            PictureSocialVO socialVO = new PictureSocialVO();
            socialVO.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
            socialVO.setFavoriteCount(stats.getFavoriteCount() != null ? stats.getFavoriteCount() : 0);
            socialVO.setShareCount(stats.getShareCount() != null ? stats.getShareCount() : 0);
            socialVO.setViewCount(stats.getViewCount() != null ? stats.getViewCount() : 0);
            socialVO.setDownloadCount(stats.getDownloadCount() != null ? stats.getDownloadCount() : 0);
            socialVO.setIsLiked(likeStatusMap.getOrDefault(picId, false));
            socialVO.setIsFavorited(favStatusMap.getOrDefault(picId, false));
            vo.setSocialInfo(socialVO);
        }
    }

    // ==================== JSON 解析辅助 ====================

    private List<PictureVO> parsePictureVORecords(String json) {
        if (StrUtil.isBlank(json) || "[]".equals(json)) {
            return Collections.emptyList();
        }
        try {
            JSONObject jsonObj = JSONUtil.parseObj(json);
            JSONArray recordsArr = jsonObj.getJSONArray("records");
            if (recordsArr == null || recordsArr.isEmpty()) {
                return Collections.emptyList();
            }
            return recordsArr.toList(PictureVO.class);
        } catch (Exception e) {
            log.warn("解析推荐缓存失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private long parseTotal(String json) {
        try {
            JSONObject jsonObj = JSONUtil.parseObj(json);
            return jsonObj.getLong("total", 0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private int nullToZero(Integer val) {
        return val == null ? 0 : val;
    }
}
