package org.example.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.PictureConstant;
import org.example.common.context.UserContext;
import org.example.common.enums.NotificationTypeEnum;
import org.example.common.enums.ReviewStatusEnum;
import org.example.common.enums.UserEnum;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.entity.Space;
import org.example.server.service.SpaceService;
import org.example.server.template.upload.FileUploadPicture;
import org.example.server.template.upload.PictureUploadTemplate;
import org.example.server.template.upload.UrlUploadPicture;
import org.example.common.util.AliOssUtil;
import org.example.common.util.EmailUtil;
import org.example.pojo.dto.picture.*;
import org.example.pojo.entity.Category;
import org.example.pojo.entity.Notification;
import org.example.pojo.entity.Picture;
import org.example.pojo.entity.User;
import org.example.pojo.vo.PictureEntityVO;
import org.example.pojo.vo.PictureSocialVO;
import org.example.pojo.vo.PictureStatisticsVO;
import org.example.pojo.vo.PictureVO;
import org.example.pojo.vo.UserVO;
import org.example.server.service.CategoryService;
import org.example.server.service.NotificationService;
import org.example.server.service.SocialService;
import org.example.server.service.PictureService;
import org.example.server.mapper.PictureMapper;
import org.example.server.service.UserService;
import org.example.server.strategy.imageSearch.ImageSearchStrategy;
import org.example.server.strategy.imageSearch.model.ImageSourceResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Zou
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-05-14 21:47:55
 */
@Slf4j
@Service("dbPictureService")
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private AliOssUtil aliOssUtils;

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private EmailUtil emailUtil;

    @Resource
    private FileUploadPicture fileUploadPicture;

    @Resource
    private UrlUploadPicture urlUploadPicture;

    // 缓存暂时禁用
//    @Resource
//    private StringRedisTemplate stringRedisTemplate;
//
//    @Resource
//    private RedisCacheUtil redisCacheUtil;

    //程序化事务控制
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource(name = "dbSocialService")
    private SocialService socialService;

    @Resource(name = "cachedNotificationService")
    private NotificationService notificationService;

    @Resource
    private List<ImageSearchStrategy> imageSearchStrategyList;

    private Map<String, ImageSearchStrategy> imageSearchStrategyMap;

    @javax.annotation.PostConstruct
    private void initStrategyMap() {
        imageSearchStrategyMap = imageSearchStrategyList.stream()
                .collect(Collectors.toMap(ImageSearchStrategy::getSourceType, s -> s));
    }

    // 本地缓存暂时禁用
//    private final Cache<String, String> LOCAL_CACHE =
//            Caffeine.newBuilder()
//                    .maximumSize(1000L)
//                    .expireAfterWrite(5L, TimeUnit.MINUTES)
//                    .build();

    //    图片到达后端
//    -》是否指定spaceId，是则校验当前操作人是否为空间所属人，如果不是则抛出错误；没有指定spaceId——不进行空间校验
//    -》图片上传
//    -》修改图片时需要删除旧图片（空间处理时是及时删除还是后置还是需要后置删除需要讨论，现在先及时删除，后续在讨论开新线程的扩展性）
//    -》管理员和空间上传自动过审
//    -》更新图片信息
//    -》更新空间信息（如果spaceId不为空）
//    -》结束
    @Override

    public Picture upload(Object inputResource, FileDTO fileDTO) throws Exception {
        Picture picture = new Picture();
        Picture oldPicture = new Picture();
        User user = UserContext.get();
        Long imageId = null;
        Long spaceId = fileDTO.getSpaceId();
        Space space;
        if (ObjUtil.isNotEmpty(spaceId)) {
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.PARAMS_ERROR, "空间不存在");
            spaceService.validAuthUser(space, user);
            picture.setSpaceId(spaceId);
        } else {
            space = null;
        }

        if (ObjUtil.isNotEmpty(fileDTO)) {
            imageId = fileDTO.getId();
        }
        //初始状态为新图片
        boolean isSaved = false;
        //已经上传过，修改状态，后续走更新操作
        if (!ObjUtil.isEmpty(imageId)) {
            picture = this.getById(imageId);
            BeanUtil.copyProperties(picture,oldPicture);
            //删除之前上传的图片
            aliOssUtils.deleteByUrl(picture.getUrl());
            isSaved = true;
        }

        //刷新用户信息
        user = userService.getById(user.getId());
        //校验空间是否支持上传
        if (ObjUtil.isNotEmpty(space)) {
            ThrowUtils.throwIf(space.getMaxSize() <= space.getTotalSize() || space.getMaxCount() <= space.getTotalCount(), ErrorCode.OPERATION_ERROR, "空间图片额度不足");
        }

        //模板方法处理文件上传和URL上传
        PictureUploadTemplate pictureUploadTemplate = fileUploadPicture;
        if (inputResource instanceof String) {
            pictureUploadTemplate = urlUploadPicture;
        }



        UploadPictureDTO uploadPictureDTO = pictureUploadTemplate.upload(inputResource);
        try {
            //审核状态：管理员/空间上传/已绑定手机号 → 自动过审；未绑定手机号 → 待审核
            UserEnum userEnum = UserEnum.getByValue(user.getUserRole());
            boolean autoPass = UserEnum.ADMIN.equals(userEnum)
                    || ObjUtil.isNotEmpty(space)
                    || StrUtil.isNotBlank(user.getUserPhone());
            if (autoPass) {
                picture.setReviewStatus(1);
                picture.setReviewMessage(ObjUtil.isNotEmpty(space) ? "空间上传自动过审" : "自动过审");
                picture.setReviewerId(user.getId());
                picture.setReviewTime(new Date());
            } else {
                picture.setReviewStatus(0);
            }
            //构建Picture存入数据库
            String name = uploadPictureDTO.getName();
            if (ObjUtil.isNotEmpty(fileDTO)) {
                if (fileDTO.getName() != null) {
                    name = fileDTO.getName();
                }
                if (fileDTO.getCategoryId() != null) {
                    validCategory(fileDTO.getCategoryId());

                    picture.setCategoryId(fileDTO.getCategoryId());
                }
                if (StrUtil.isNotBlank(fileDTO.getTags())) {
                    picture.setTags(fileDTO.getTags());
                }
                if (StrUtil.isNotBlank(fileDTO.getIntroduction())) {
                    picture.setIntroduction(fileDTO.getIntroduction());
                }
            }
            picture.setName(name);
            picture.setUserId(user.getId());
            picture.setUrl(uploadPictureDTO.getUrl());
            picture.setThumbnailUrl(uploadPictureDTO.getThumbnailUrl());
            picture.setOriginUrl(uploadPictureDTO.getOriginUrl());
            picture.setPicWidth(uploadPictureDTO.getPicWidth());
            picture.setPicHeight(uploadPictureDTO.getPicHeight());
            picture.setPicScale(uploadPictureDTO.getPicScale());
            picture.setPicFormat(uploadPictureDTO.getPicFormat());
            picture.setPicSize(uploadPictureDTO.getPicSize());

            if (isSaved) {
                //更新图片信息之前先更新空间额度
                Picture finalPicture1 = picture;
                picture = transactionTemplate.execute(status -> {
                    boolean result = this.updateById(finalPicture1);
                    ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"图片上传失败");
                    if (ObjUtil.isNotEmpty(space)){
                        boolean update = spaceService.lambdaUpdate()
                                .eq(Space::getId,space.getId())
                                .setSql(String.format("totalSize = totalSize + %s", finalPicture1.getPicSize() - oldPicture.getPicSize()))
                                .update();
                        ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"空间额度更新失败");
                    }
                    return finalPicture1;
                });

            } else {
                //控制图片插入和空间额度更改
                Picture finalPicture = picture;
                picture = transactionTemplate.execute(status -> {
                    boolean result = this.save(finalPicture);
                    ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"图片上传失败");
                    if (ObjUtil.isNotEmpty(space)){
                        boolean update = spaceService.lambdaUpdate()
                                .eq(Space::getId,space.getId())
                                .setSql(String.format("totalSize = totalSize + %s",finalPicture.getPicSize()))
                                .setSql("totalCount = totalCount + 1")
                                .update();
                        ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"空间额度更新失败");
                    }
                    return finalPicture;
                });

            }

            return picture;
        } catch (Exception e) {
            aliOssUtils.deleteByUrl(uploadPictureDTO.getUrl());
            log.error("图片上传失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
    }

    @Override
    public byte[] download(Picture picture) throws ClientException, FileNotFoundException {
        //判空留给controller
        //拿到url，下载文件至本地
        String url = picture.getOriginUrl();
        String name = picture.getName();
        Long id = UserContext.get().getId();
        aliOssUtils.download(url, name, id);
        //通过File拿到本地文件，转换为字节流返回
        File filePath = new File(PictureConstant.TEMP_FILE_URL);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        String localFilePath = PictureConstant.TEMP_FILE_URL + id + "_" + name;
        File localFile = new File(localFilePath);
        FileInputStream inputStream = new FileInputStream(localFile);
        return IoUtil.readBytes(inputStream);
    }

    @Override
    public boolean updatePicture(PictureUpdateDTO pictureUpdateDTO) {
        //转化实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateDTO, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateDTO.getTags()));
        //校验图片数据
        validPicture(picture);
        //校验分类是否存在
        validCategory(picture.getCategoryId());

        //判断图片是否存在
        Picture oldPicture = this.getById(pictureUpdateDTO.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.PARAMS_ERROR, "图片不存在");

        //更新
        return this.updateById(picture);
    }
//根据图片Id找到picture
// -》判断用户权限（如果picture的spaceId不为null，则只有空间所有人才能操作）
// -》数据库操作-》更新空间信息
// -》返回结果
    @Override
    public boolean editPicture(PictureEditDTO pictureEditDTO) {
        //转化实体类
        User user = UserContext.get();
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditDTO, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditDTO.getTags()));
        picture.setEditTime(new Date());
        validPicture(picture);

        if (ObjUtil.isNotEmpty(picture.getSpaceId())){
            Space space = spaceService.getById(picture.getSpaceId());
            spaceService.validAuthUser(space,user);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(pictureEditDTO.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.PARAMS_ERROR, "图片不存在");

        //校验权限（只有本人和管理员才有资格修改）

        if (!user.getId().equals(oldPicture.getUserId()) && !UserEnum.ADMIN.getValue().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //校验分类是否存在
        validCategory(picture.getCategoryId());
        //更新
        return this.updateById(picture);
    }

    //2.管理员删除图片
    @Override
    public Boolean deletePicture(long id) throws Exception {
        //先查id对应图片
        User user = UserContext.get();
        Picture picture = new Picture();
        picture = this.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR, "图片不存在");
        if (ObjUtil.isNotEmpty(picture.getSpaceId())){
            Space space = spaceService.getById(picture.getSpaceId());
            spaceService.validAuthUser(space,user);
        }

        //仅管理员或自己可删除图片
        if (!user.getId().equals(picture.getUserId()) && !UserEnum.ADMIN.getValue().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //拿到url删除oss上数据
        String url = picture.getUrl();
        aliOssUtils.deleteByUrl(url);

        //事务管理删除图片和清理空间内图片
        Picture finalPicture = picture;
        //删除数据库内数据
        return transactionTemplate.execute(status -> {
            boolean remove = this.removeById(finalPicture);
            ThrowUtils.throwIf(!remove,ErrorCode.OPERATION_ERROR);
            if (finalPicture.getSpaceId() != null){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId,finalPicture.getSpaceId())
                        .setSql(String.format("totalSize = totalSize - %s", finalPicture.getPicSize()))
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR);

            }
            return true;
        });
    }


    //3.分页查询图片（管理员/普通用户）

    /**
     * 管理员分页查询
     */
    @Override
    public Page<PictureEntityVO> queryPictureListAdmin(PictureQueryDTO queryDTO) {
        queryDTO.setNullSpaceId(false);
        //构建查询条件
        QueryWrapper queryWrapper = getQueryWrapper(queryDTO);
        //Page封装
        Page<Picture> pictureList = this.page(new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize()), queryWrapper);
        List<Picture> pictures = pictureList.getRecords();

        Page<PictureEntityVO> result = new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize(), pictureList.getTotal());

        if (ObjUtil.isEmpty(pictures)) {
            return result;
        }
        List<PictureEntityVO> pictureEntityVOList = pictures.stream()
                .map(picture -> {
                    PictureEntityVO pictureEntityVO = new PictureEntityVO();
                    BeanUtil.copyProperties(picture, pictureEntityVO);
                    return pictureEntityVO;
                })
                .toList();
        // 批量填充分类名称
        Set<Long> categoryIds = pictureEntityVOList.stream()
                .map(PictureEntityVO::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : categoryService.listByIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        pictureEntityVOList.forEach(pictureEntityVO -> {
            Long categoryId = pictureEntityVO.getCategoryId();
            if (categoryId != null && categoryMap.containsKey(categoryId)) {
                pictureEntityVO.setCategoryName(categoryMap.get(categoryId).getName());
            }
        });
        result = result.setRecords(pictureEntityVOList);

        return result;
    }

    /**
     * 用户分页查询
     */
    @Override
    public Page<PictureVO> queryPictureListUser(PictureQueryDTO queryDTO) {
        Long spaceId = queryDTO.getSpaceId();
        queryDTO.setNullSpaceId(!ObjUtil.isNotEmpty(spaceId));

        // 空间图片列表需校验请求者权限
        if (ObjUtil.isNotEmpty(spaceId)) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.PARAMS_ERROR, "空间不存在");
            spaceService.validAuthUser(space, UserContext.get());
        }

        // 缓存暂时禁用，直接查询数据库
//        String key = JSONUtil.toJsonStr(queryDTO);
//        String hashKey = "queryPictureListUser:" + DigestUtils.md5DigestAsHex(key.getBytes());
//        int ttlSeconds = 300 + RandomUtil.randomInt(0, 600);
//        String cacheResult = redisCacheUtil.getWithLock(hashKey, ttlSeconds, () -> {

        QueryWrapper<Picture> queryWrapper = getQueryWrapper(queryDTO);
        Page<Picture> pictureList = this.page(new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize()), queryWrapper);
        List<Picture> pictures = pictureList.getRecords();

        Page<PictureVO> result = new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize(), pictureList.getTotal());
        if (ObjUtil.isEmpty(pictures)) {
            return result;
        }
        List<PictureVO> pictureVOList = pictures.stream()
                .map(PictureVO::objToVO)
                .toList();

        // 批量填充用户信息
        Set<Long> userIds = pictureVOList.stream()
                .map(PictureVO::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserMapList = userService.listByIds(userIds)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        // 批量填充分类名称
        Set<Long> categoryIds = pictureVOList.stream()
                .map(PictureVO::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : categoryService.listByIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserMapList.containsKey(userId)) {
                user = userIdUserMapList.get(userId).get(0);
            }
            UserVO userVO = new UserVO();
            BeanUtil.copyProperties(user, userVO);
            pictureVO.setUserVO(userVO);

            Long categoryId = pictureVO.getCategoryId();
            if (categoryId != null && categoryMap.containsKey(categoryId)) {
                pictureVO.setCategoryName(categoryMap.get(categoryId).getName());
            }

        });
        // 填充社交数据（仅公共图库图片）
        fillSocialData(pictureVOList);
        result = result.setRecords(pictureVOList);

//            return JSONUtil.toJsonStr(result);
//        });
//
//        if (cacheResult == null) {
//            return new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
//        }
//        return JSONUtil.toBean(cacheResult, Page.class);
        return result;
    }


    //4.根据id获取图片信息（管理员/普通用户）

    /**
     * 管理员查看图片信息
     */
    @Override
    public Picture getByPictureIdAdmin(long id) {
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR, "图片不存在");

        return picture;
    }


    /**
     * 用户查看图片信息
     */
//    请求到达后端
//    -》找到对应picture
//    -》picture中的spaceId不为空则需要进行权限校验，非空间所有人或则非管理员不能查看该图片
    @Override
    public PictureVO getByPictureIdUser(long id) {
//        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
//        //只获取审批通过的图片
//        queryWrapper.eq("id", id);
//        queryWrapper.eq("reviewStatus", 1);
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR, "图片不存在");

        if (ObjUtil.isNotEmpty(picture.getSpaceId())){
            Space space = spaceService.getById(picture.getSpaceId());
            spaceService.validAuthUser(space,UserContext.get());
        }
        PictureVO pictureVO = getPictureVO(picture);

        // 填充社交数据（仅公共图库）
        if (picture.getSpaceId() == null) {
            User currentUser = UserContext.get();
            Long currentUserId = currentUser != null ? currentUser.getId() : null;
            List<Long> ids = Collections.singletonList(id);
            Map<Long, PictureStatisticsVO> statsMap = socialService.batchStatistics(ids);
            PictureStatisticsVO stats = statsMap.getOrDefault(id, new PictureStatisticsVO());

            PictureSocialVO socialVO = new PictureSocialVO();
            socialVO.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
            socialVO.setFavoriteCount(stats.getFavoriteCount() != null ? stats.getFavoriteCount() : 0);
            socialVO.setShareCount(stats.getShareCount() != null ? stats.getShareCount() : 0);
            socialVO.setViewCount(stats.getViewCount() != null ? stats.getViewCount() : 0);
            socialVO.setDownloadCount(stats.getDownloadCount() != null ? stats.getDownloadCount() : 0);
            if (currentUserId != null) {
                Map<Long, Boolean> likeStatus = socialService.batchLikeStatus(ids, currentUserId);
                Map<Long, Boolean> favStatus = socialService.batchFavoriteStatus(ids, currentUserId);
                socialVO.setIsLiked(likeStatus.getOrDefault(id, false));
                socialVO.setIsFavorited(favStatus.getOrDefault(id, false));
            } else {
                socialVO.setIsLiked(false);
                socialVO.setIsFavorited(false);
            }
            pictureVO.setSocialInfo(socialVO);
        }

        return pictureVO;
    }

    /**
     * 图片审批
     */
    @Override
    public void pictureReview(PictureReviewDTO pictureReviewDTO) {
        //获取图片信息
        Picture oldPicture = this.getById(pictureReviewDTO.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldPicture), ErrorCode.PARAMS_ERROR, "图片不存在");
        //获取当前操作员
        User user = UserContext.get();
        //校验图片信息状态(当操作状态和当前状态相同时返回)
        ReviewStatusEnum reviewStatusEnum = ReviewStatusEnum.getByValue(oldPicture.getReviewStatus());
        ThrowUtils.throwIf(reviewStatusEnum.equals(ReviewStatusEnum.getByValue(pictureReviewDTO.getReviewStatus())), ErrorCode.PARAMS_ERROR, "请勿重复操作");
        //数据封装
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewDTO, picture);
        picture.setReviewerId(user.getId());
        picture.setReviewTime(new Date());
        //数据库操作
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        //审批成功后发送邮件通知和站内通知给上传者
        try {
            Long uploaderId = oldPicture.getUserId();
            if (uploaderId != null) {
                User uploader = userService.getById(uploaderId);
                boolean passed = ReviewStatusEnum.PASS.getValue() == pictureReviewDTO.getReviewStatus();
                String statusText = passed ? "通过" : "未通过";
                String reviewMessage = pictureReviewDTO.getReviewMessage();
                if (StrUtil.isBlank(reviewMessage)) {
                    reviewMessage = passed ? "恭喜，您的图片已通过审核！" : "很遗憾，您的图片未通过审核，请修改后重新上传。";
                }

                // 发送站内通知
                Notification notification = new Notification();
                notification.setReceiverId(uploaderId);
                notification.setSenderId(user.getId());
                notification.setSenderName(user.getUserName());
                notification.setType(NotificationTypeEnum.REVIEW.getType());
                notification.setTitle("图片审批" + statusText);
                notification.setContent("您的图片《" + oldPicture.getName() + "》审批" + statusText + "。"
                        + (StrUtil.isNotBlank(pictureReviewDTO.getReviewMessage())
                            ? "审核意见：" + pictureReviewDTO.getReviewMessage() : ""));
                notification.setResourceId(oldPicture.getId());
                notification.setResourceUrl(oldPicture.getUrl());
                notification.setIsRead(0);
                notificationService.save(notification);

                // 发送邮件通知
                if (uploader != null && StrUtil.isNotBlank(uploader.getUserEmail())) {
                    emailUtil.sendReviewNotice(
                            uploader.getUserEmail(),
                            oldPicture.getName(),
                            passed,
                            pictureReviewDTO.getReviewMessage()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("审批通知发送异常, pictureId={}, error={}", pictureReviewDTO.getId(), e.getMessage());
        }
    }

    @Override
    public Integer pictureUploadByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO) {
        //校验数据非空
        String searchText = pictureUploadByBatchDTO.getSearchText();
        int count = pictureUploadByBatchDTO.getCount();
        String namePrefix = pictureUploadByBatchDTO.getProfile();
        Long categoryId = pictureUploadByBatchDTO.getCategoryId();
        List<String> tagList = pictureUploadByBatchDTO.getTags();
        String searchSource = pictureUploadByBatchDTO.getSearchSource();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索词不能为空");
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        //根据 searchSource 选择策略
        ImageSearchStrategy strategy = imageSearchStrategyMap.get(searchSource);
        ThrowUtils.throwIf(strategy == null, ErrorCode.PARAMS_ERROR, "不支持的搜索来源: " + searchSource);

        //使用策略搜索图片
        List<ImageSourceResult> imageResults = strategy.searchImages(searchText, count);
        int uploadCount = 0;

        for (ImageSourceResult result : imageResults) {
            //上传图片
            FileDTO fileDTO = new FileDTO();
            if (StrUtil.isNotBlank(result.getName())) {
                fileDTO.setName(result.getName());
            } else if (StrUtil.isNotBlank(namePrefix)) {
                fileDTO.setName(namePrefix + (uploadCount + 1));
            }
            if (StrUtil.isNotBlank(result.getIntroduction())) {
                fileDTO.setIntroduction(result.getIntroduction());
            }
            if (ObjUtil.isNotEmpty(categoryId)) {
                fileDTO.setCategoryId(categoryId);
            }
            if (ObjUtil.isNotEmpty(tagList)) {
                String tags = JSONUtil.toJsonStr(tagList);
                fileDTO.setTags(tags);
            }
            try {
                Picture picture = this.upload(result.getUrl(), fileDTO);
                log.info("图片上传成功,id = {}", picture.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败，错误信息 = {}", result.getUrl(),e);
            }
            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }

    // 缓存模式暂时禁用
//    @Override
//    public Page<PictureVO> queryPictureListUserCache(PictureQueryDTO queryDTO) {
//        //构造本地缓存 Key
//        String key = JSONUtil.toJsonStr(queryDTO);
//        String hashKey = "queryPictureListUser:" + DigestUtils.md5DigestAsHex(key.getBytes());
//        //本地缓存查询
//        String cacheValue = LOCAL_CACHE.getIfPresent(hashKey);
//        if (ObjUtil.isNotEmpty(cacheValue)) {
//            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
//            return cachePage;
//        }
//
//        //查找数据库数据
//        Page<PictureVO> pictureVOPage = queryPictureListUser(queryDTO);
//        //存入本地缓存
//        cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//
//        LOCAL_CACHE.put(hashKey, cacheValue);
//
//        //返回数据
//        return pictureVOPage;
//    }

    /**
     * 绑定手机号后自动过审最新 100 张待审批图片
     */
    @Override
    public int autoApprovePicturesByBindPhone(Long userId) {
        // 查询 reviewStatus=0 的图片，按创建时间倒序，最多 100 张
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id")
                .eq("userId", userId)
                .eq("reviewStatus", 0)
                .orderByDesc("createTime")
                .last("LIMIT 100");

        List<Picture> pictures = this.list(queryWrapper);
        if (ObjUtil.isEmpty(pictures)) {
            return 0;
        }

        List<Long> pictureIds = pictures.stream()
                .map(Picture::getId)
                .collect(Collectors.toList());

        // 批量更新：单条 SQL 完成
        Date now = new Date();
        Picture update = new Picture();
        update.setReviewStatus(1);
        update.setReviewMessage("手机号绑定自动过审");
        update.setReviewerId(userId);
        update.setReviewTime(now);

        QueryWrapper<Picture> updateWrapper = new QueryWrapper<>();
        updateWrapper.in("id", pictureIds)
                .eq("reviewStatus", 0);
        boolean result = this.update(update, updateWrapper);
        int count = result ? pictureIds.size() : 0;

        log.info("绑定手机号自动过审：userId={}, count={}", userId, count);
        return count;
    }

    /**
     * 查询当前用户待审批的图片列表（直接查 DB，限定当前用户）
     */
    @Override
    public Page<PictureVO> queryPendingPictures(PictureQueryDTO queryDTO) {
        User currentUser = UserContext.get();
        ThrowUtils.throwIf(ObjUtil.isEmpty(currentUser), ErrorCode.NOT_LOGIN_ERROR);

        // 强制限定为当前用户且 reviewStatus != 1
        queryDTO.setUserId(currentUser.getId());
        queryDTO.setReviewStatus(null); // 不使用单一状态过滤，改为 ne

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", currentUser.getId());
        queryWrapper.ne("reviewStatus", 1);
        queryWrapper.orderByDesc("createTime");

        Page<Picture> pictureList = this.page(
                new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize()), queryWrapper);
        List<Picture> pictures = pictureList.getRecords();

        Page<PictureVO> result = new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize(), pictureList.getTotal());
        if (ObjUtil.isEmpty(pictures)) {
            return result;
        }

        List<PictureVO> pictureVOList = pictures.stream()
                .map(PictureVO::objToVO)
                .toList();

        // 批量填充用户信息
        Set<Long> userIds = pictureVOList.stream()
                .map(PictureVO::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserMapList = userService.listByIds(userIds)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        // 批量填充分类名称
        Set<Long> categoryIds = pictureVOList.stream()
                .map(PictureVO::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : categoryService.listByIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserMapList.containsKey(userId)) {
                user = userIdUserMapList.get(userId).get(0);
            }
            UserVO userVO = new UserVO();
            BeanUtil.copyProperties(user, userVO);
            pictureVO.setUserVO(userVO);

            Long categoryId = pictureVO.getCategoryId();
            if (categoryId != null && categoryMap.containsKey(categoryId)) {
                pictureVO.setCategoryName(categoryMap.get(categoryId).getName());
            }
        });

        result = result.setRecords(pictureVOList);
        return result;
    }

    /**
     * 验证图片信息是否正常
     */
    private void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 校验分类是否存在
     */
    private void validCategory(Long categoryId) {
        if (categoryId != null) {
            Category category = categoryService.getById(categoryId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(category), ErrorCode.PARAMS_ERROR, "分类不存在");
        }
    }


    /**
     * 构建查询条件
     */
    private QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO) {
        // 输入校验：防 DoS
        List<String> tags = pictureQueryDTO.getTags();
        ThrowUtils.throwIf(tags != null && tags.size() > 20, ErrorCode.PARAMS_ERROR, "标签数量不能超过20个");
        String searchText = pictureQueryDTO.getSearchText();
        ThrowUtils.throwIf(StrUtil.isNotBlank(searchText) && searchText.length() > 200,
                ErrorCode.PARAMS_ERROR, "搜索文本不能超过200个字符");

        Long id = pictureQueryDTO.getId();
        String name = pictureQueryDTO.getName();
        String introduction = pictureQueryDTO.getIntroduction();
        Long categoryId = pictureQueryDTO.getCategoryId();
        Long picSize = pictureQueryDTO.getPicSize();
        Integer picWidth = pictureQueryDTO.getPicWidth();
        Integer picHeight = pictureQueryDTO.getPicHeight();
        Double picScale = pictureQueryDTO.getPicScale();
        Long userId = pictureQueryDTO.getUserId();
        Long spaceId = pictureQueryDTO.getSpaceId();
        Boolean nullSpaceId = pictureQueryDTO.getNullSpaceId();
        //审核状态
        Integer reviewStatus = pictureQueryDTO.getReviewStatus();
        Date startEditTime = pictureQueryDTO.getStartEditTime();
        Date endEditTime = pictureQueryDTO.getEndEditTime();

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }

        if (ObjUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.eq(ObjUtil.isNotEmpty(categoryId), "categoryId", categoryId);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.orderBy(!ObjUtil.isEmpty(pictureQueryDTO.getSortField()), "ascend".equals(pictureQueryDTO.getSortOrder()), pictureQueryDTO.getSortField());
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        return queryWrapper;
    }

    /**
     * 批量填充社交数据（仅公共图库图片）
     */
    private void fillSocialData(List<PictureVO> pictureVOList) {
        // 过滤出公共图库图片（spaceId == null）
        List<Long> publicIds = pictureVOList.stream()
                .filter(vo -> vo.getSpaceId() == null)
                .map(PictureVO::getId)
                .collect(Collectors.toList());
        if (publicIds.isEmpty()) {
            return;
        }

        // 获取当前用户
        User currentUser = UserContext.get();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        // 批量获取统计数据
        Map<Long, PictureStatisticsVO> statsMap = socialService.batchStatistics(publicIds);

        // 批量获取点赞/收藏状态
        Map<Long, Boolean> likeStatusMap = currentUserId != null
                ? socialService.batchLikeStatus(publicIds, currentUserId)
                : Collections.emptyMap();
        Map<Long, Boolean> favStatusMap = currentUserId != null
                ? socialService.batchFavoriteStatus(publicIds, currentUserId)
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


    /**
     * 获取图像用户信息
     */
    private PictureVO getPictureVO(Picture picture) {
        PictureVO pictureVO = PictureVO.objToVO(picture);

        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = new UserVO();
            BeanUtil.copyProperties(user, userVO);
            pictureVO.setUserVO(userVO);
        }

        Long categoryId = picture.getCategoryId();
        if (categoryId != null && categoryId > 0) {
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                pictureVO.setCategoryName(category.getName());
            }
        }

        return pictureVO;
    }

}
