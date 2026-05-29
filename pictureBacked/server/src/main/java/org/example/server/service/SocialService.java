package org.example.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.pojo.dto.social.UserPictureQueryDTO;
import org.example.pojo.vo.PictureBriefVO;
import org.example.pojo.vo.PictureStatisticsVO;
import org.example.pojo.vo.ToggleFavoriteVO;
import org.example.pojo.vo.ToggleLikeVO;

import java.util.List;
import java.util.Map;

/**
 * 社交功能 Service
 * 点赞、收藏、分享
 *
 * @author Zou
 */
public interface SocialService {

    /**
     * 点赞/取消点赞（toggle）
     *
     * @param pictureId 图片 id
     * @param userId    用户 id
     * @return 点赞结果
     */
    ToggleLikeVO toggleLike(Long pictureId, Long userId);

    /**
     * 批量获取点赞状态
     *
     * @param pictureIds 图片 id 列表
     * @param userId     用户 id
     * @return pictureId -> liked
     */
    Map<Long, Boolean> batchLikeStatus(List<Long> pictureIds, Long userId);

    /**
     * 收藏/取消收藏（toggle）
     *
     * @param pictureId 图片 id
     * @param userId    用户 id
     * @return 收藏结果
     */
    ToggleFavoriteVO toggleFavorite(Long pictureId, Long userId);

    /**
     * 批量获取收藏状态
     *
     * @param pictureIds 图片 id 列表
     * @param userId     用户 id
     * @return pictureId -> favorited
     */
    Map<Long, Boolean> batchFavoriteStatus(List<Long> pictureIds, Long userId);

    /**
     * 记录分享行为（仅增加分享计数）
     *
     * @param pictureId 图片 id
     */
    void recordShare(Long pictureId);

    /**
     * 记录浏览行为（仅增加浏览计数）
     *
     * @param pictureId 图片 id
     */
    void incrementViewCount(Long pictureId);

    /**
     * 记录下载行为（仅增加下载计数）
     *
     * @param pictureId 图片 id
     */
    void incrementDownloadCount(Long pictureId);

    /**
     * 批量获取统计数据（likeCount, favoriteCount, shareCount, viewCount, downloadCount）
     *
     * @param pictureIds 图片 id 列表
     * @return pictureId -> 统计数据
     */
    Map<Long, PictureStatisticsVO> batchStatistics(List<Long> pictureIds);

    /**
     * 获取用户点赞的图片列表（分页 + 筛选）
     *
     * @param userId   目标用户 id
     * @param queryDTO 查询条件（分页、筛选、排序）
     * @return 分页结果
     */
    Page<PictureBriefVO> getUserLikedPictures(Long userId, UserPictureQueryDTO queryDTO);

    /**
     * 获取用户收藏的图片列表（分页 + 筛选）
     *
     * @param userId   目标用户 id
     * @param queryDTO 查询条件（分页、筛选、排序）
     * @return 分页结果
     */
    Page<PictureBriefVO> getUserFavoritedPictures(Long userId, UserPictureQueryDTO queryDTO);

    /**
     * 获取用户上传的公共图片列表（分页）
     *
     * @param userId   目标用户 id
     * @param queryDTO 查询条件（分页）
     * @return 分页结果
     */
    Page<PictureBriefVO> getUserUploadedPictures(Long userId, UserPictureQueryDTO queryDTO);
}