package org.example.server.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.annotation.CheckAuth;
import org.example.common.api.imagesearch.ImageSearchApiFacade;
import org.example.common.api.imagesearch.model.ImageSearchResult;
import org.example.common.constants.UserConstant;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.context.UserContext;
import org.example.pojo.entity.User;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.common.util.AliOssUtil;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.picture.*;
import org.example.pojo.dto.social.BatchStatusQueryDTO;
import org.example.pojo.dto.social.UserPictureQueryDTO;

import org.example.pojo.entity.Picture;
import org.example.pojo.vo.PictureBriefVO;
import org.example.pojo.vo.PictureEntityVO;
import org.example.pojo.vo.PictureVO;
import org.example.pojo.vo.ToggleFavoriteVO;
import org.example.pojo.vo.ToggleLikeVO;
import org.example.server.service.PictureService;
import org.example.server.service.SocialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/picture")
public class PictureController {

    private static final Logger log = LoggerFactory.getLogger(PictureController.class);
    @Resource
    private PictureService pictureService;

    @Resource
    private AliOssUtil aliOssUtils;

    @Resource
    private SocialService socialService;

    /**
     * 图片上传
     */
//    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @PostMapping("/upload")
    public BaseResponse<PictureVO> upload(
            @RequestParam("files") MultipartFile file,
            FileDTO fileDTO) throws Exception {
        //文件判空
        ThrowUtils.throwIf(ObjUtil.isEmpty(file), ErrorCode.PARAMS_ERROR, "文件不能为空");

        Picture picture = pictureService.upload(file, fileDTO);

        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);

        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> upload(@RequestBody FileDTO fileDTO) throws Exception {
        //文件判空
        ThrowUtils.throwIf(ObjUtil.isEmpty(fileDTO), ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.upload(fileDTO.getFileUrl(), fileDTO);

        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);

        return ResultUtils.success(pictureVO);
    }

    /**
     * 图片下载
     */
    @GetMapping("/download")
    public void download(Long id, HttpServletResponse response) throws IOException, ClientException {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR);
        try {
            //字节流
            byte[] result = pictureService.download(picture);
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
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDTO pictureUpdateDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUpdateDTO), ErrorCode.PARAMS_ERROR);

        boolean result = pictureService.updatePicture(pictureUpdateDTO);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDTO pictureEditDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureEditDTO), ErrorCode.PARAMS_ERROR);

        boolean result = pictureService.editPicture(pictureEditDTO);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);
    }

    //2.删除图片
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest) throws Exception {
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest), ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        boolean result = pictureService.deletePicture(id);

        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);

    }

    //3.分页查询图片（管理员/普通用户）
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @PostMapping("/admin/query")
    public BaseResponse<Page<PictureEntityVO>> queryPictureAdmin(@RequestBody PictureQueryDTO pictureQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO), ErrorCode.PARAMS_ERROR);

        Page<PictureEntityVO> result = pictureService.queryPictureListAdmin(pictureQueryDTO);

        return ResultUtils.success(result);
    }

    @PostMapping("/user/query")
    public BaseResponse<Page<PictureVO>> queryPictureUser(@RequestBody PictureQueryDTO pictureQueryDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO), ErrorCode.PARAMS_ERROR);
        pictureQueryDTO.setReviewStatus(1);
        Page<PictureVO> result = pictureService.queryPictureListUser(pictureQueryDTO);

        return ResultUtils.success(result);
    }

    // 缓存接口暂时禁用
//    @PostMapping("/user/query/cache")
//    public BaseResponse<Page<PictureVO>> queryPictureUserCache(@RequestBody PictureQueryDTO pictureQueryDTO) {
//        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO), ErrorCode.PARAMS_ERROR);
//        ThrowUtils.throwIf(pictureQueryDTO.getPageSize() > 20,ErrorCode.PARAMS_ERROR);
//        pictureQueryDTO.setReviewStatus(1);
//        Page<PictureVO> result = pictureService.queryPictureListUserCache(pictureQueryDTO);
//
//        return ResultUtils.success(result);
//    }

    //4.根据id获取图片信息（管理员/普通用户）
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @GetMapping("/admin/{id}")
    public BaseResponse<Picture> getPictureByIdAdmin(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.getByPictureIdAdmin(id);

        return ResultUtils.success(picture);
    }

    //5.获取图片详细信息
    @GetMapping("/user/{id}")
    public BaseResponse<PictureVO> getPictureByIdUser(@PathVariable Long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);

        PictureVO pictureVO = pictureService.getByPictureIdUser(id);

        return ResultUtils.success(pictureVO);
    }

    //6.图片审批
    @PostMapping("/review")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewDTO pictureReviewDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureReviewDTO), ErrorCode.PARAMS_ERROR);

        pictureService.pictureReview(pictureReviewDTO);

        return ResultUtils.success(true);
    }


    @PostMapping("/upload/batch")
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    public BaseResponse<Integer> pictureUploadByBatch(@RequestBody PictureUploadByBatchDTO pictureUploadByBatchDTO) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUploadByBatchDTO), ErrorCode.PARAMS_ERROR);

        ThrowUtils.throwIf(pictureUploadByBatchDTO.getCount() > 30, ErrorCode.PARAMS_ERROR, "最多一次抓取 30 张图片");
        int result = pictureService.pictureUploadByBatch(pictureUploadByBatchDTO);

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
        Picture oldPicture = pictureService.getById(pictureId);
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
        ToggleLikeVO result = socialService.toggleLike(pictureId, user.getId());
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
        Map<Long, Boolean> result = socialService.batchLikeStatus(pictureIds, userId);
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
        ToggleFavoriteVO result = socialService.toggleFavorite(pictureId, user.getId());
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
        Map<Long, Boolean> result = socialService.batchFavoriteStatus(pictureIds, userId);
        return ResultUtils.success(result);
    }

    /**
     * 记录分享行为
     */
    @PostMapping("/share/{pictureId}")
    public BaseResponse<Boolean> recordShare(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        socialService.recordShare(pictureId);
        return ResultUtils.success(true);
    }

    /**
     * 记录浏览行为
     */
    @PostMapping("/view/{pictureId}")
    public BaseResponse<Boolean> recordView(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        socialService.incrementViewCount(pictureId);
        return ResultUtils.success(true);
    }

    /**
     * 记录下载行为
     */
    @PostMapping("/download/count/{pictureId}")
    public BaseResponse<Boolean> recordDownloadCount(@PathVariable Long pictureId) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        socialService.incrementDownloadCount(pictureId);
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
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        Page<PictureBriefVO> result = socialService.getUserLikedPictures(userId, queryDTO);
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
        Page<PictureBriefVO> result = socialService.getUserFavoritedPictures(userId, queryDTO);
        return ResultUtils.success(result);
    }

}