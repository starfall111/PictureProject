package org.example.server.controller.benchmark;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.social.BatchStatusQueryDTO;
import org.example.pojo.dto.social.UserPictureQueryDTO;
import org.example.pojo.vo.PictureBriefVO;
import org.example.pojo.vo.ToggleFavoriteVO;
import org.example.pojo.vo.ToggleLikeVO;
import org.example.server.service.SocialService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 社交功能（点赞/收藏）压测专用 Controller
 *
 * <p>仅在 {@code spring.profiles.active=benchmark} 时加载，生产环境不会注册此 Bean。
 * 跳过 session 鉴权，通过请求参数直接传入 userId，完整复用 Redis + MQ 社交链路。</p>
 *
 * <p>提供两套接口用于对比：
 * <ul>
 *   <li>{@code /benchmark/social/...} — 缓存版（Redis 同步 + MQ 异步写 DB），即线上主链路</li>
 *   <li>{@code /benchmark/social/db/...} — 纯 DB 版（同步写 DB），作为对照基准</li>
 * </ul>
 * </p>
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/benchmark/social")
@Profile("benchmark")
public class SocialBenchmarkController {

    @Resource(name = "cachedSocialService")
    private SocialService cachedSocialService;

    @Resource(name = "dbSocialService")
    private SocialService dbSocialService;

    // ==================== 缓存版（Redis + MQ） ====================

    /**
     * [缓存版] 点赞/取消点赞
     */
    @PostMapping("/like/{pictureId}")
    public BaseResponse<ToggleLikeVO> toggleLikeCache(
            @PathVariable Long pictureId,
            @RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "userId 不合法");
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "pictureId 不合法");
        ToggleLikeVO result = cachedSocialService.toggleLike(pictureId, userId);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 收藏/取消收藏
     */
    @PostMapping("/favorite/{pictureId}")
    public BaseResponse<ToggleFavoriteVO> toggleFavoriteCache(
            @PathVariable Long pictureId,
            @RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "userId 不合法");
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "pictureId 不合法");
        ToggleFavoriteVO result = cachedSocialService.toggleFavorite(pictureId, userId);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 批量获取点赞状态
     */
    @PostMapping("/like/status")
    public BaseResponse<Map<Long, Boolean>> batchLikeStatusCache(
            @RequestBody BatchStatusQueryDTO request,
            @RequestParam Long userId) {
        List<Long> pictureIds = request.getPictureIds();
        ThrowUtils.throwIf(pictureIds == null || pictureIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        Map<Long, Boolean> result = cachedSocialService.batchLikeStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 批量获取收藏状态
     */
    @PostMapping("/favorite/status")
    public BaseResponse<Map<Long, Boolean>> batchFavoriteStatusCache(
            @RequestBody BatchStatusQueryDTO request,
            @RequestParam Long userId) {
        List<Long> pictureIds = request.getPictureIds();
        ThrowUtils.throwIf(pictureIds == null || pictureIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        Map<Long, Boolean> result = cachedSocialService.batchFavoriteStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    // ==================== 纯 DB 版（对照基准） ====================

    /**
     * [DB 版] 点赞/取消点赞
     */
    @PostMapping("/db/like/{pictureId}")
    public BaseResponse<ToggleLikeVO> toggleLikeDb(
            @PathVariable Long pictureId,
            @RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "userId 不合法");
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "pictureId 不合法");
        ToggleLikeVO result = dbSocialService.toggleLike(pictureId, userId);
        return ResultUtils.success(result);
    }

    /**
     * [DB 版] 收藏/取消收藏
     */
    @PostMapping("/db/favorite/{pictureId}")
    public BaseResponse<ToggleFavoriteVO> toggleFavoriteDb(
            @PathVariable Long pictureId,
            @RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "userId 不合法");
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "pictureId 不合法");
        ToggleFavoriteVO result = dbSocialService.toggleFavorite(pictureId, userId);
        return ResultUtils.success(result);
    }

    /**
     * [DB 版] 批量获取点赞状态
     */
    @PostMapping("/db/like/status")
    public BaseResponse<Map<Long, Boolean>> batchLikeStatusDb(
            @RequestBody BatchStatusQueryDTO request,
            @RequestParam Long userId) {
        List<Long> pictureIds = request.getPictureIds();
        ThrowUtils.throwIf(pictureIds == null || pictureIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        Map<Long, Boolean> result = dbSocialService.batchLikeStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    /**
     * [DB 版] 批量获取收藏状态
     */
    @PostMapping("/db/favorite/status")
    public BaseResponse<Map<Long, Boolean>> batchFavoriteStatusDb(
            @RequestBody BatchStatusQueryDTO request,
            @RequestParam Long userId) {
        List<Long> pictureIds = request.getPictureIds();
        ThrowUtils.throwIf(pictureIds == null || pictureIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        Map<Long, Boolean> result = dbSocialService.batchFavoriteStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    // ==================== 列表查询 ====================

    /**
     * [缓存版] 获取用户点赞的图片列表
     */
    @PostMapping("/liked/user/{userId}/query")
    public BaseResponse<Page<PictureBriefVO>> getUserLikedPictures(
            @PathVariable Long userId,
            @RequestBody UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "userId 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        Page<PictureBriefVO> result = cachedSocialService.getUserLikedPictures(userId, queryDTO);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 获取用户收藏的图片列表
     */
    @PostMapping("/favorited/user/{userId}/query")
    public BaseResponse<Page<PictureBriefVO>> getUserFavoritedPictures(
            @PathVariable Long userId,
            @RequestBody UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "userId 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        Page<PictureBriefVO> result = cachedSocialService.getUserFavoritedPictures(userId, queryDTO);
        return ResultUtils.success(result);
    }
}
