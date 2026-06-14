package org.example.server.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import org.example.common.annotation.CheckAuth;
import org.example.common.annotation.RateLimit;
import org.example.common.annotation.RateLimitDimension;
import org.example.common.api.imagesearch.ImageSearchApiFacade;
import org.example.common.api.imagesearch.model.ImageSearchResult;
import org.example.common.constants.UserConstant;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.context.UserContext;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.enums.UserEnum;
import org.example.pojo.entity.User;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.common.util.AliOssUtil;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.picture.*;
import org.example.pojo.dto.social.BatchStatusQueryDTO;
import org.example.pojo.dto.social.UserPictureQueryDTO;

import org.example.pojo.entity.Picture;
import org.example.pojo.entity.Space;
import org.example.pojo.vo.BatchTaskVO;
import org.example.pojo.vo.PictureBriefVO;
import org.example.pojo.vo.PictureEntityVO;
import org.example.pojo.vo.PictureVO;
import org.example.pojo.vo.ToggleFavoriteVO;
import org.example.pojo.vo.ToggleLikeVO;
import org.example.server.service.BatchTaskService;
import org.example.server.service.PictureService;
import org.example.server.service.SpaceService;
import org.example.server.service.SocialService;
import org.example.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.*;

@RestController
@RequestMapping("/picture")
public class PictureController {

    private static final Logger log = LoggerFactory.getLogger(PictureController.class);
    @Resource(name = "dbPictureService")
    private PictureService pictureService;

    @Resource
    private AliOssUtil aliOssUtils;

    @Resource(name = "dbSocialService")
    private SocialService socialService;

    @Resource(name = "cachedSocialService")
    private SocialService cachedSocialService;

    @Resource(name = "cachedPictureService")
    private PictureService cachedPictureService;

    @Resource
    private BatchTaskService batchTaskService;

    @Resource
    private SpaceService spaceService;

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 图片上传
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(resource = "picture.upload", dimensions = {RateLimitDimension.USER})
    public BaseResponse<PictureVO> upload(
            @RequestPart("file") MultipartFile file,
            FileDTO fileDTO) throws Exception {
        //认证检查
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);
        //文件判空
        ThrowUtils.throwIf(ObjUtil.isEmpty(file), ErrorCode.PARAMS_ERROR, "文件不能为空");

        Picture picture = cachedPictureService.upload(file, fileDTO);

        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);

        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    @RateLimit(resource = "picture.uploadUrl", dimensions = {RateLimitDimension.USER})
    public BaseResponse<PictureVO> upload(@RequestBody FileDTO fileDTO) throws Exception {
        //认证检查
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);
        //文件判空
        ThrowUtils.throwIf(ObjUtil.isEmpty(fileDTO), ErrorCode.PARAMS_ERROR);

        Picture picture = cachedPictureService.upload(fileDTO.getFileUrl(), fileDTO);

        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);

        return ResultUtils.success(pictureVO);
    }

    /**
     * 图片下载
     */
    @GetMapping("/download")
    @RateLimit(resource = "picture.download", dimensions = {RateLimitDimension.USER})
    public void download(Long id, HttpServletResponse response) throws IOException, ClientException {
        //认证检查
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        Picture picture = cachedPictureService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR);
        try {
            //字节流
            byte[] result = cachedPictureService.download(picture);
            String fileName = picture.getName();
            //设置响应头
            response.setContentType("application/octet-stream:charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            //写入响应
            response.getOutputStream().write(result);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filename is " + picture.getName());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

    }

    //1.图片编辑信息：更改图片信息（管理员/普通用户）
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @PostMapping("/update")
    @RateLimit(resource = "picture.update", dimensions = {RateLimitDimension.USER})
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDTO pictureUpdateDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUpdateDTO), ErrorCode.PARAMS_ERROR);

        boolean result = cachedPictureService.updatePicture(pictureUpdateDTO);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDTO pictureEditDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureEditDTO), ErrorCode.PARAMS_ERROR);

        boolean result = cachedPictureService.editPicture(pictureEditDTO);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);
    }

    //2.删除图片
    @DeleteMapping("/delete")
    @RateLimit(resource = "picture.delete", dimensions = {RateLimitDimension.USER})
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest) throws Exception {
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest), ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        boolean result = cachedPictureService.deletePicture(id);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);

    }

    //3.分页查询图片（管理员/普通用户）
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @PostMapping("/admin/query")
    @RateLimit(resource = "picture.queryAdmin", dimensions = {RateLimitDimension.USER})
    public BaseResponse<Page<PictureEntityVO>> queryPictureAdmin(@RequestBody PictureQueryDTO pictureQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO), ErrorCode.PARAMS_ERROR);

        Page<PictureEntityVO> result = cachedPictureService.queryPictureListAdmin(pictureQueryDTO);

        return ResultUtils.success(result);
    }

    @PostMapping("/user/query")
    @RateLimit(resource = "picture.queryUser", dimensions = {RateLimitDimension.USER})
    public BaseResponse<Page<PictureVO>> queryPictureUser(@RequestBody PictureQueryDTO pictureQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO), ErrorCode.PARAMS_ERROR);
        pictureQueryDTO.setReviewStatus(1);
        Page<PictureVO> result = cachedPictureService.queryPictureListUser(pictureQueryDTO);

        return ResultUtils.success(result);
    }

    // 缓存接口暂时禁用
//    @PostMapping("/user/query/cache")
//    public BaseResponse<Page<PictureVO>> queryPictureUserCache(@RequestBody PictureQueryDTO pictureQueryDTO) {
//        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO), ErrorCode.PARAMS_ERROR);
//        ThrowUtils.throwIf(pictureQueryDTO.getPageSize() > 20,ErrorCode.PARAMS_ERROR);
//        pictureQueryDTO.setReviewStatus(1);
//        Page<PictureVO> result = cachedPictureService.queryPictureListUserCache(pictureQueryDTO);
//
//        return ResultUtils.success(result);
//    }

    //4.根据id获取图片信息（管理员/普通用户）
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @GetMapping("/admin/{id}")
    public BaseResponse<Picture> getPictureByIdAdmin(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        Picture picture = cachedPictureService.getByPictureIdAdmin(id);

        return ResultUtils.success(picture);
    }

    //5.获取图片详细信息
    @GetMapping("/user/{id}")
    public BaseResponse<PictureVO> getPictureByIdUser(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        PictureVO pictureVO = cachedPictureService.getByPictureIdUser(id);

        return ResultUtils.success(pictureVO);
    }

    //6.图片审批
    @PostMapping("/review")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewDTO pictureReviewDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureReviewDTO), ErrorCode.PARAMS_ERROR);

        cachedPictureService.pictureReview(pictureReviewDTO);

        return ResultUtils.success(true);
    }


    @PostMapping("/upload/batch")
    @RateLimit(resource = "picture.batchUpload", dimensions = {RateLimitDimension.USER})
    public BaseResponse<BatchTaskVO> pictureUploadByBatch(@RequestBody PictureUploadByBatchDTO pictureUploadByBatchDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUploadByBatchDTO), ErrorCode.PARAMS_ERROR);

        // 获取当前用户
        User user = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_LOGIN_ERROR);
        user = userService.getById(user.getId());

        // 同一用户同时只能进行一个批量任务
        ThrowUtils.throwIf(batchTaskService.hasRunningTask(user.getId()),
                ErrorCode.OPERATION_ERROR, "您有正在进行的批量任务，请等待完成后再提交");

        // 权限校验：管理员直接通过
        UserEnum userEnum = UserEnum.getByValue(user.getUserRole());
        if (!UserEnum.ADMIN.equals(userEnum)) {
            // TODO: VIP 用户校验，后续实现兑换码功能
            // 已绑定手机号可通过
            ThrowUtils.throwIf(StrUtil.isBlank(user.getUserPhone()),
                    ErrorCode.NO_AUTH_ERROR, "请先绑定手机号");
        }

        // 限流：每用户每分钟最多 1 次
        String rateLimitKey = String.format(RedisKeyConstants.BATCH_TASK_RATE_LIMIT_KEY, user.getId());
        Long count = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(rateLimitKey, RedisKeyConstants.BATCH_TASK_RATE_LIMIT_WINDOW, TimeUnit.SECONDS);
        }
        ThrowUtils.throwIf(count != null && count > RedisKeyConstants.BATCH_TASK_RATE_LIMIT_MAX,
                ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");

        // 参数校验
        ThrowUtils.throwIf(pictureUploadByBatchDTO.getCount() > 30, ErrorCode.PARAMS_ERROR, "最多一次抓取 30 张图片");
        ThrowUtils.throwIf(StrUtil.isBlank(pictureUploadByBatchDTO.getSearchText()), ErrorCode.PARAMS_ERROR, "搜索词不能为空");

        // 空间额度校验
        if (ObjUtil.isNotEmpty(pictureUploadByBatchDTO.getSpaceId())) {
            Space space = spaceService.getById(pictureUploadByBatchDTO.getSpaceId());
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.PARAMS_ERROR, "空间不存在");
            spaceService.validAuthUser(space, user);
            long remaining = space.getMaxCount() - space.getTotalCount();
            ThrowUtils.throwIf(remaining < pictureUploadByBatchDTO.getCount(),
                    ErrorCode.OPERATION_ERROR,
                    String.format("空间图片数量不足，剩余 %d 张额度，需要 %d 张", remaining, pictureUploadByBatchDTO.getCount()));
        }

        // 创建异步任务
        BatchTaskVO taskVO = batchTaskService.createAndSubmitTask(user, pictureUploadByBatchDTO);

        return ResultUtils.success(taskVO);
    }


    /**
     * 查询当前用户待审批的图片列表
     */
    @PostMapping("/user/pending/query")
    public BaseResponse<Page<PictureVO>> queryPendingPictures(@RequestBody PictureQueryDTO pictureQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO), ErrorCode.PARAMS_ERROR);
        Page<PictureVO> result = cachedPictureService.queryPendingPictures(pictureQueryDTO);
        return ResultUtils.success(result);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureDTO searchPictureByPictureDTO) {
        ThrowUtils.throwIf(searchPictureByPictureDTO == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureDTO.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = cachedPictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }


    // ==================== 社交功能接口 ====================

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/like/{pictureId}")
    public BaseResponse<ToggleLikeVO> toggleLike(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ToggleLikeVO result = cachedSocialService.toggleLike(pictureId, user.getId());
        return ResultUtils.success(result);
    }

    /**
     * 批量获取点赞状态
     */
    @PostMapping("/like/status")
    public BaseResponse<Map<Long, Boolean>> batchLikeStatus(@RequestBody BatchStatusQueryDTO request) {
        List<Long> pictureIds = request.getPictureIds();
        ThrowUtils.throwIf(pictureIds == null || pictureIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        User user = UserContext.get();
        Long userId = user != null ? user.getId() : null;
        Map<Long, Boolean> result = cachedSocialService.batchLikeStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    /**
     * 收藏/取消收藏
     */
    @PostMapping("/favorite/{pictureId}")
    public BaseResponse<ToggleFavoriteVO> toggleFavorite(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ToggleFavoriteVO result = cachedSocialService.toggleFavorite(pictureId, user.getId());
        return ResultUtils.success(result);
    }

    /**
     * 批量获取收藏状态
     */
    @PostMapping("/favorite/status")
    public BaseResponse<Map<Long, Boolean>> batchFavoriteStatus(@RequestBody BatchStatusQueryDTO request) {
        List<Long> pictureIds = request.getPictureIds();
        ThrowUtils.throwIf(pictureIds == null || pictureIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        User user = UserContext.get();
        Long userId = user != null ? user.getId() : null;
        Map<Long, Boolean> result = cachedSocialService.batchFavoriteStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    /**
     * 记录分享行为
     */
    @PostMapping("/share/{pictureId}")
    public BaseResponse<Boolean> recordShare(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        cachedSocialService.recordShare(pictureId);
        return ResultUtils.success(true);
    }

    /**
     * 记录浏览行为
     */
    @PostMapping("/view/{pictureId}")
    public BaseResponse<Boolean> recordView(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        cachedSocialService.incrementViewCount(pictureId);
        return ResultUtils.success(true);
    }

    /**
     * 记录下载行为
     */
    @PostMapping("/download/count/{pictureId}")
    public BaseResponse<Boolean> recordDownloadCount(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        cachedSocialService.incrementDownloadCount(pictureId);
        return ResultUtils.success(true);
    }

    /**
     * 获取用户点赞的图片列表（支持筛选）
     */
    @PostMapping("/liked/user/{userId}/query")
    public BaseResponse<Page<PictureBriefVO>> getUserLikedPictures(
            @PathVariable Long userId,
            @RequestBody UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        Page<PictureBriefVO> result = cachedSocialService.getUserLikedPictures(userId, queryDTO);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户收藏的图片列表（支持筛选）
     */
    @PostMapping("/favorited/user/{userId}/query")
    public BaseResponse<Page<PictureBriefVO>> getUserFavoritedPictures(
            @PathVariable Long userId,
            @RequestBody UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        Page<PictureBriefVO> result = cachedSocialService.getUserFavoritedPictures(userId, queryDTO);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户关注对象的图片列表（支持筛选）
     */
    @PostMapping("/following/user/query")
    public BaseResponse<Page<PictureBriefVO>> getFollowingPictures(
            @RequestBody UserPictureQueryDTO queryDTO) {
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        Page<PictureBriefVO> result = cachedSocialService.getFollowingPictures(user.getId(), queryDTO);
        return ResultUtils.success(result);
    }

    // ==================== 缓存版接口（/cache/ 前缀） ====================

    /**
     * [缓存版] 图片列表查询
     */
    @PostMapping("/cache/user/query")
    public BaseResponse<Page<PictureVO>> queryPictureUserCache(@RequestBody PictureQueryDTO pictureQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO), ErrorCode.PARAMS_ERROR);
        pictureQueryDTO.setReviewStatus(1);
        Page<PictureVO> result = cachedPictureService.queryPictureListUser(pictureQueryDTO);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 图片详情查询
     */
    @GetMapping("/cache/user/detail/{id}")
    public BaseResponse<PictureVO> getPictureByIdUserCache(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        PictureVO pictureVO = cachedPictureService.getByPictureIdUser(id);
        return ResultUtils.success(pictureVO);
    }

    /**
     * [缓存版] 点赞/取消点赞
     */
    @PostMapping("/cache/like/{pictureId}")
    public BaseResponse<ToggleLikeVO> toggleLikeCache(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ToggleLikeVO result = cachedSocialService.toggleLike(pictureId, user.getId());
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 批量获取点赞状态
     */
    @PostMapping("/cache/like/status")
    public BaseResponse<Map<Long, Boolean>> batchLikeStatusCache(@RequestBody BatchStatusQueryDTO request) {
        List<Long> pictureIds = request.getPictureIds();
        ThrowUtils.throwIf(pictureIds == null || pictureIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        User user = UserContext.get();
        Long userId = user != null ? user.getId() : null;
        Map<Long, Boolean> result = cachedSocialService.batchLikeStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 收藏/取消收藏
     */
    @PostMapping("/cache/favorite/{pictureId}")
    public BaseResponse<ToggleFavoriteVO> toggleFavoriteCache(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ToggleFavoriteVO result = cachedSocialService.toggleFavorite(pictureId, user.getId());
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 批量获取收藏状态
     */
    @PostMapping("/cache/favorite/status")
    public BaseResponse<Map<Long, Boolean>> batchFavoriteStatusCache(@RequestBody BatchStatusQueryDTO request) {
        List<Long> pictureIds = request.getPictureIds();
        ThrowUtils.throwIf(pictureIds == null || pictureIds.isEmpty(), ErrorCode.PARAMS_ERROR);
        User user = UserContext.get();
        Long userId = user != null ? user.getId() : null;
        Map<Long, Boolean> result = cachedSocialService.batchFavoriteStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 记录分享行为
     */
    @PostMapping("/cache/share/{pictureId}")
    public BaseResponse<Boolean> recordShareCache(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        cachedSocialService.recordShare(pictureId);
        return ResultUtils.success(true);
    }

    /**
     * [缓存版] 记录浏览行为
     */
    @PostMapping("/cache/view/{pictureId}")
    public BaseResponse<Boolean> recordViewCache(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        cachedSocialService.incrementViewCount(pictureId);
        return ResultUtils.success(true);
    }

    /**
     * [缓存版] 记录下载行为
     */
    @PostMapping("/cache/download/count/{pictureId}")
    public BaseResponse<Boolean> recordDownloadCountCache(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        cachedSocialService.incrementDownloadCount(pictureId);
        return ResultUtils.success(true);
    }

    /**
     * [缓存版] 获取用户点赞的图片列表（不支持筛选）
     */
    @PostMapping("/cache/liked/user/{userId}/query")
    public BaseResponse<Page<PictureBriefVO>> getUserLikedPicturesCache(
            @PathVariable Long userId,
            @RequestBody UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        // 隐私保护：只能查看自己的列表
//        verifyOwnershipOrAdmin(userId);
        Page<PictureBriefVO> result = cachedSocialService.getUserLikedPictures(userId, queryDTO);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 获取用户收藏的图片列表（不支持筛选）
     */
    @PostMapping("/cache/favorited/user/{userId}/query")
    public BaseResponse<Page<PictureBriefVO>> getUserFavoritedPicturesCache(
            @PathVariable Long userId,
            @RequestBody UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        // 隐私保护：只能查看自己的列表
//        verifyOwnershipOrAdmin(userId);
        Page<PictureBriefVO> result = cachedSocialService.getUserFavoritedPictures(userId, queryDTO);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 获取用户上传的图片列表（不支持筛选）
     */
    @PostMapping("/cache/uploaded/user/{userId}/query")
    public BaseResponse<Page<PictureBriefVO>> getUserUploadedPicturesCache(
            @PathVariable Long userId,
            @RequestBody UserPictureQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        // 隐私保护：只能查看自己的列表
//        verifyOwnershipOrAdmin(userId);
        Page<PictureBriefVO> result = cachedSocialService.getUserUploadedPictures(userId, queryDTO);
        return ResultUtils.success(result);
    }

    /**
     * [缓存版] 获取当前用户关注对象的图片列表（不支持筛选）
     */
    @PostMapping("/cache/following/user/query")
    public BaseResponse<Page<PictureBriefVO>> getFollowingPicturesCache(
            @RequestBody UserPictureQueryDTO queryDTO) {
        User user = UserContext.get();
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        Page<PictureBriefVO> result = cachedSocialService.getFollowingPictures(user.getId(), queryDTO);
        return ResultUtils.success(result);
    }
}