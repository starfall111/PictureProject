package org.example.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.dto.social.UserPictureQueryDTO;
import org.example.pojo.entity.Picture;
import org.example.pojo.entity.PictureBrief;
import org.example.pojo.entity.PictureFavorite;
import org.example.pojo.entity.PictureLike;
import org.example.pojo.entity.PictureStatistics;
import org.example.pojo.entity.User;
import org.example.pojo.vo.PictureBriefVO;
import org.example.pojo.vo.PictureStatisticsVO;
import org.example.pojo.vo.ToggleFavoriteVO;
import org.example.pojo.vo.ToggleLikeVO;
import org.example.pojo.vo.UserVO;
import org.example.server.mapper.*;
import org.example.server.service.SocialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 社交功能 Service 实现（不含缓存，直接操作数据库）
 *
 * @author Zou
 */
@Slf4j
@Service("dbSocialService")
public class SocialServiceImpl implements SocialService {

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
    private TransactionTemplate transactionTemplate;

    @Resource
    private UserFollowMapper userFollowMapper;

    /**
     * 本地锁，防止同一用户对同一图片的 toggle 操作并发执行
     * key = "userId:pictureId"
     */
    private final ConcurrentHashMap<String, Object> toggleLocks = new ConcurrentHashMap<>();

    @Override
    public ToggleLikeVO toggleLike(Long pictureId, Long userId) {
        validPicturePublic(pictureId);

        String lockKey = userId + ":" + pictureId;
        Object lock = toggleLocks.computeIfAbsent(lockKey, k -> new Object());

        boolean liked;
        try {
            synchronized (lock) {
                QueryWrapper<PictureLike> qw = new QueryWrapper<>();
                qw.eq("pictureId", pictureId).eq("userId", userId);
                PictureLike existing = pictureLikeMapper.selectOne(qw);

                if (existing != null) {
                    pictureLikeMapper.deleteById(existing.getId());
                    liked = false;
                } else {
                    PictureLike pictureLike = new PictureLike();
                    pictureLike.setPictureId(pictureId);
                    pictureLike.setUserId(userId);
                    pictureLikeMapper.insert(pictureLike);
                    liked = true;
                }
            }
        } finally {
            toggleLocks.remove(lockKey);
        }

        int likeCount = updateLikeCount(pictureId);

        ToggleLikeVO result = new ToggleLikeVO();
        result.setLiked(liked);
        result.setLikeCount(likeCount);
        return result;
    }

    @Override
    public Map<Long, Boolean> batchLikeStatus(List<Long> pictureIds, Long userId) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (userId == null) {
            return pictureIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        QueryWrapper<PictureLike> qw = new QueryWrapper<>();
        qw.in("pictureId", pictureIds).eq("userId", userId);
        List<PictureLike> likes = pictureLikeMapper.selectList(qw);

        Map<Long, Boolean> result = pictureIds.stream()
                .collect(Collectors.toMap(id -> id, id -> false));
        for (PictureLike like : likes) {
            result.put(like.getPictureId(), true);
        }
        return result;
    }

    @Override
    public ToggleFavoriteVO toggleFavorite(Long pictureId, Long userId) {
        validPicturePublic(pictureId);

        String lockKey = userId + ":" + pictureId;
        Object lock = toggleLocks.computeIfAbsent(lockKey, k -> new Object());

        boolean favorited;
        try {
            synchronized (lock) {
                QueryWrapper<PictureFavorite> qw = new QueryWrapper<>();
                qw.eq("pictureId", pictureId).eq("userId", userId);
                PictureFavorite existing = pictureFavoriteMapper.selectOne(qw);

                if (existing != null) {
                    pictureFavoriteMapper.deleteById(existing.getId());
                    favorited = false;
                } else {
                    PictureFavorite pictureFavorite = new PictureFavorite();
                    pictureFavorite.setPictureId(pictureId);
                    pictureFavorite.setUserId(userId);
                    pictureFavoriteMapper.insert(pictureFavorite);
                    favorited = true;
                }
            }
        } finally {
            toggleLocks.remove(lockKey);
        }

        int favoriteCount = updateFavoriteCount(pictureId);

        ToggleFavoriteVO result = new ToggleFavoriteVO();
        result.setFavorited(favorited);
        result.setFavoriteCount(favoriteCount);
        return result;
    }

    @Override
    public Map<Long, Boolean> batchFavoriteStatus(List<Long> pictureIds, Long userId) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return Collections.emptyMap();
        }
        if (userId == null) {
            return pictureIds.stream().collect(Collectors.toMap(id -> id, id -> false));
        }

        QueryWrapper<PictureFavorite> qw = new QueryWrapper<>();
        qw.in("pictureId", pictureIds).eq("userId", userId);
        List<PictureFavorite> favorites = pictureFavoriteMapper.selectList(qw);

        Map<Long, Boolean> result = pictureIds.stream()
                .collect(Collectors.toMap(id -> id, id -> false));
        for (PictureFavorite fav : favorites) {
            result.put(fav.getPictureId(), true);
        }
        return result;
    }

    @Override
    public void recordShare(Long pictureId) {
        validPicturePublic(pictureId);
        incrementShareCount(pictureId);
    }

    @Override
    public void incrementViewCount(Long pictureId) {
        PictureStatistics stat = pictureStatisticsMapper.selectById(pictureId);
        if (stat == null) {
            stat = new PictureStatistics();
            stat.setPictureId(pictureId);
            stat.setLikeCount(0);
            stat.setFavoriteCount(0);
            stat.setShareCount(0);
            stat.setViewCount(1);
            stat.setDownloadCount(0);
            pictureStatisticsMapper.insert(stat);
        } else {
            stat.setViewCount((stat.getViewCount() != null ? stat.getViewCount() : 0) + 1);
            pictureStatisticsMapper.updateById(stat);
        }
    }

    @Override
    public void incrementDownloadCount(Long pictureId) {
        PictureStatistics stat = pictureStatisticsMapper.selectById(pictureId);
        if (stat == null) {
            stat = new PictureStatistics();
            stat.setPictureId(pictureId);
            stat.setLikeCount(0);
            stat.setFavoriteCount(0);
            stat.setShareCount(0);
            stat.setViewCount(0);
            stat.setDownloadCount(1);
            pictureStatisticsMapper.insert(stat);
        } else {
            stat.setDownloadCount((stat.getDownloadCount() != null ? stat.getDownloadCount() : 0) + 1);
            pictureStatisticsMapper.updateById(stat);
        }
    }

    @Override
    public Map<Long, PictureStatisticsVO> batchStatistics(List<Long> pictureIds) {
        if (pictureIds == null || pictureIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QueryWrapper<PictureStatistics> qw = new QueryWrapper<>();
        qw.in("pictureId", pictureIds);
        List<PictureStatistics> stats = pictureStatisticsMapper.selectList(qw);

        Map<Long, PictureStatisticsVO> result = new HashMap<>();
        for (Long pictureId : pictureIds) {
            result.put(pictureId, defaultStats());
        }
        for (PictureStatistics stat : stats) {
            PictureStatisticsVO vo = new PictureStatisticsVO();
            vo.setLikeCount(stat.getLikeCount() != null ? stat.getLikeCount() : 0);
            vo.setFavoriteCount(stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0);
            vo.setShareCount(stat.getShareCount() != null ? stat.getShareCount() : 0);
            vo.setViewCount(stat.getViewCount() != null ? stat.getViewCount() : 0);
            vo.setDownloadCount(stat.getDownloadCount() != null ? stat.getDownloadCount() : 0);
            result.put(stat.getPictureId(), vo);
        }
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 批量填充图片上传者用户信息
     */
    private void fillUserInfo(List<PictureBriefVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream()
                .map(PictureBriefVO::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (PictureBriefVO vo : records) {
            User user = userMap.get(vo.getUserId());
            if (user != null) {
                UserVO userVO = new UserVO();
                cn.hutool.core.bean.BeanUtil.copyProperties(user, userVO);
                vo.setUserVO(userVO);
            }
        }
    }

    private void validPicturePublic(Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片 id 不合法");
        Picture picture = pictureMapper.selectById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(picture.getSpaceId() != null, ErrorCode.NO_AUTH_ERROR, "仅公共图库支持社交功能");
    }

    private int updateLikeCount(Long pictureId) {
        QueryWrapper<PictureLike> countQw = new QueryWrapper<>();
        countQw.eq("pictureId", pictureId);
        int likeCount = pictureLikeMapper.selectCount(countQw).intValue();

        PictureStatistics stat = pictureStatisticsMapper.selectById(pictureId);
        if (stat == null) {
            stat = new PictureStatistics();
            stat.setPictureId(pictureId);
            stat.setLikeCount(likeCount);
            stat.setFavoriteCount(0);
            stat.setShareCount(0);
            stat.setViewCount(0);
            stat.setDownloadCount(0);
            pictureStatisticsMapper.insert(stat);
        } else {
            stat.setLikeCount(likeCount);
            pictureStatisticsMapper.updateById(stat);
        }
        return likeCount;
    }

    private int updateFavoriteCount(Long pictureId) {
        QueryWrapper<PictureFavorite> countQw = new QueryWrapper<>();
        countQw.eq("pictureId", pictureId);
        int favoriteCount = pictureFavoriteMapper.selectCount(countQw).intValue();

        PictureStatistics stat = pictureStatisticsMapper.selectById(pictureId);
        if (stat == null) {
            stat = new PictureStatistics();
            stat.setPictureId(pictureId);
            stat.setLikeCount(0);
            stat.setFavoriteCount(favoriteCount);
            stat.setShareCount(0);
            stat.setViewCount(0);
            stat.setDownloadCount(0);
            pictureStatisticsMapper.insert(stat);
        } else {
            stat.setFavoriteCount(favoriteCount);
            pictureStatisticsMapper.updateById(stat);
        }
        return favoriteCount;
    }

    private void incrementShareCount(Long pictureId) {
        PictureStatistics stat = pictureStatisticsMapper.selectById(pictureId);
        if (stat == null) {
            stat = new PictureStatistics();
            stat.setPictureId(pictureId);
            stat.setLikeCount(0);
            stat.setFavoriteCount(0);
            stat.setShareCount(1);
            stat.setViewCount(0);
            stat.setDownloadCount(0);
            pictureStatisticsMapper.insert(stat);
        } else {
            stat.setShareCount((stat.getShareCount() != null ? stat.getShareCount() : 0) + 1);
            pictureStatisticsMapper.updateById(stat);
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

    @Override
    public Page<PictureBriefVO> getUserLikedPictures(Long userId, UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");

        Integer current = queryDTO.getCurrent();
        Integer pageSize = queryDTO.getPageSize();
        Integer offset = (current - 1) * pageSize;

        // 查询列表
        List<PictureBrief> briefList = pictureLikeMapper.selectUserLikedPictures(userId, queryDTO, offset, pageSize);
        // PictureBrief → PictureBriefVO（String tags → List<String> tags）
        List<PictureBriefVO> records = briefList.stream().map(PictureBriefVO::objToVO).collect(Collectors.toList());

        // 批量填充用户信息
        fillUserInfo(records);

        // 查询总数
        Long total = pictureLikeMapper.countUserLikedPictures(userId, queryDTO);

        // 组装分页结果
        Page<PictureBriefVO> page = new Page<>(current, pageSize);
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    public Page<PictureBriefVO> getUserFavoritedPictures(Long userId, UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");

        Integer current = queryDTO.getCurrent();
        Integer pageSize = queryDTO.getPageSize();
        Integer offset = (current - 1) * pageSize;

        // 查询列表
        List<PictureBrief> briefList = pictureFavoriteMapper.selectUserFavoritedPictures(userId, queryDTO, offset, pageSize);
        // PictureBrief → PictureBriefVO（String tags → List<String> tags）
        List<PictureBriefVO> records = briefList.stream().map(PictureBriefVO::objToVO).collect(Collectors.toList());

        // 批量填充用户信息
        fillUserInfo(records);

        // 查询总数
        Long total = pictureFavoriteMapper.countUserFavoritedPictures(userId, queryDTO);

        // 组装分页结果
        Page<PictureBriefVO> page = new Page<>(current, pageSize);
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    public Page<PictureBriefVO> getUserUploadedPictures(Long userId, UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");

        Integer current = queryDTO.getCurrent();
        Integer pageSize = queryDTO.getPageSize();

        QueryWrapper<Picture> qw = new QueryWrapper<>();
        qw.eq("userId", userId)
                .eq("reviewStatus", 1)
                .isNull("spaceId")
                .orderByDesc("editTime");

        Page<Picture> picturePage = pictureMapper.selectPage(new Page<>(current, pageSize), qw);
        List<PictureBriefVO> records = picturePage.getRecords().stream()
                .map(picture -> {
                    PictureBriefVO vo = new PictureBriefVO();
                    vo.setId(picture.getId());
                    vo.setName(picture.getName());
                    vo.setUrl(picture.getUrl());
                    vo.setThumbnailUrl(picture.getThumbnailUrl());
                    vo.setPicWidth(picture.getPicWidth());
                    vo.setPicHeight(picture.getPicHeight());
                    vo.setCreateTime(picture.getCreateTime());
                    vo.setUserId(picture.getUserId());
                    if (picture.getTags() != null) {
                        vo.setTags(cn.hutool.json.JSONUtil.toList(picture.getTags(), String.class));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        Page<PictureBriefVO> page = new Page<>(current, pageSize, picturePage.getTotal());
        page.setRecords(records);

        // 批量填充用户信息
        fillUserInfo(records);

        return page;
    }

    @Override
    public Page<PictureBriefVO> getFollowingPictures(Long userId, UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");

        // 查询关注用户 ID 列表
        List<Long> followeeIds = userFollowMapper.selectFolloweeIds(userId);
        if (followeeIds == null || followeeIds.isEmpty()) {
            return new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize(), 0);
        }

        Integer current = queryDTO.getCurrent();
        Integer pageSize = queryDTO.getPageSize();

        QueryWrapper<Picture> qw = new QueryWrapper<>();
        qw.in("userId", followeeIds)
                .eq("reviewStatus", 1)
                .isNull("spaceId")
                .orderByDesc("editTime");

        Page<Picture> picturePage = pictureMapper.selectPage(new Page<>(current, pageSize), qw);
        List<PictureBriefVO> records = picturePage.getRecords().stream()
                .map(picture -> {
                    PictureBriefVO vo = new PictureBriefVO();
                    vo.setId(picture.getId());
                    vo.setName(picture.getName());
                    vo.setUrl(picture.getUrl());
                    vo.setThumbnailUrl(picture.getThumbnailUrl());
                    vo.setPicWidth(picture.getPicWidth());
                    vo.setPicHeight(picture.getPicHeight());
                    vo.setCreateTime(picture.getCreateTime());
                    vo.setUserId(picture.getUserId());
                    if (picture.getTags() != null) {
                        vo.setTags(cn.hutool.json.JSONUtil.toList(picture.getTags(), String.class));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        Page<PictureBriefVO> page = new Page<>(current, pageSize, picturePage.getTotal());
        page.setRecords(records);

        // 批量填充用户信息
        fillUserInfo(records);

        return page;
    }
}