package org.example.picture.core.infrastructure.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.example.identity.api.port.UserContentStatsPort;
import org.example.identity.api.port.model.CategoryBrief;
import org.example.identity.api.port.model.UserContentStats;
import org.example.picture.core.application.CategoryService;
import org.example.picture.core.domain.model.Category;
import org.example.picture.core.domain.model.Picture;
import org.example.picture.core.domain.model.PictureStatistics;
import org.example.picture.core.infrastructure.persistence.PictureMapper;
import org.example.picture.core.infrastructure.persistence.PictureStatisticsMapper;
import org.example.picture.social.infrastructure.persistence.PictureFavoriteMapper;
import org.example.picture.social.infrastructure.persistence.PictureLikeMapper;
import org.example.picture.social.interfaces.dto.UserPictureQueryDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户内容统计适配器 — 实现身份上下文的端口（防腐层实现）
 * <p>
 * 逻辑平移自原 UserServiceImpl#getUserProfile 的统计部分。
 *
 * @author Zou
 */
@Component
public class UserContentStatsAdapter implements UserContentStatsPort {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private PictureStatisticsMapper pictureStatisticsMapper;

    @Resource
    private PictureLikeMapper pictureLikeMapper;

    @Resource
    private PictureFavoriteMapper pictureFavoriteMapper;

    @Resource
    private CategoryService categoryService;

    @Override
    public UserContentStats getUserContentStats(Long userId) {
        UserContentStats stats = new UserContentStats();

        // 1. 统计用户点赞和收藏的图片数量
        UserPictureQueryDTO emptyQuery = new UserPictureQueryDTO();
        Long userLikeCount = pictureLikeMapper.countUserLikedPictures(userId, emptyQuery);
        Long userFavoriteCount = pictureFavoriteMapper.countUserFavoritedPictures(userId, emptyQuery);
        stats.setUserLikeCount(userLikeCount != null ? userLikeCount.intValue() : 0);
        stats.setUserFavoriteCount(userFavoriteCount != null ? userFavoriteCount.intValue() : 0);

        // 2. 统计公共图库上传数量
        QueryWrapper<Picture> pictureQw = new QueryWrapper<>();
        pictureQw.eq("userId", userId)
                .isNull("spaceId")
                .eq("reviewStatus", 1);
        Integer uploadCount = pictureMapper.selectCount(pictureQw).intValue();
        stats.setUploadCount(uploadCount);

        // 3. 如果没有公共图片，返回零统计数据
        if (uploadCount == 0) {
            stats.setTotalLikes(0);
            stats.setTotalFavorites(0);
            stats.setTotalViews(0);
            stats.setTotalShares(0);
            stats.setTotalDownloads(0);
            stats.setCategories(new ArrayList<>());
            return stats;
        }

        // 4. 获取用户所有公共图片 ID 列表
        List<Picture> pictures = pictureMapper.selectList(pictureQw);
        List<Long> pictureIds = pictures.stream()
                .map(Picture::getId)
                .collect(Collectors.toList());

        // 5. 查询这些图片的统计数据并聚合
        QueryWrapper<PictureStatistics> statsQw = new QueryWrapper<>();
        statsQw.in("pictureId", pictureIds);
        List<PictureStatistics> statisticsList = pictureStatisticsMapper.selectList(statsQw);

        int totalLikes = 0;
        int totalFavorites = 0;
        int totalViews = 0;
        int totalShares = 0;
        int totalDownloads = 0;

        for (PictureStatistics stat : statisticsList) {
            totalLikes += stat.getLikeCount() != null ? stat.getLikeCount() : 0;
            totalFavorites += stat.getFavoriteCount() != null ? stat.getFavoriteCount() : 0;
            totalViews += stat.getViewCount() != null ? stat.getViewCount() : 0;
            totalShares += stat.getShareCount() != null ? stat.getShareCount() : 0;
            totalDownloads += stat.getDownloadCount() != null ? stat.getDownloadCount() : 0;
        }

        stats.setTotalLikes(totalLikes);
        stats.setTotalFavorites(totalFavorites);
        stats.setTotalViews(totalViews);
        stats.setTotalShares(totalShares);
        stats.setTotalDownloads(totalDownloads);

        // 6. 查询用户图片涉及的分类
        QueryWrapper<Picture> categoryQw = new QueryWrapper<>();
        categoryQw.select("DISTINCT categoryId")
                .eq("userId", userId)
                .isNull("spaceId")
                .eq("reviewStatus", 1)
                .isNotNull("categoryId");
        List<Picture> categoryPictures = pictureMapper.selectList(categoryQw);

        List<Long> categoryIds = categoryPictures.stream()
                .map(Picture::getCategoryId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        if (categoryIds.isEmpty()) {
            stats.setCategories(new ArrayList<>());
            return stats;
        }

        // 批量获取分类信息
        List<Category> categories = categoryService.listByIds(categoryIds);
        List<CategoryBrief> categoryBriefList = categories.stream()
                .map(cat -> new CategoryBrief(cat.getId(), cat.getName()))
                .collect(Collectors.toList());
        stats.setCategories(categoryBriefList);

        return stats;
    }
}
