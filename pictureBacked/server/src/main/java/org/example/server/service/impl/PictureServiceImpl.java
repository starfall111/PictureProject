package org.example.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.PictureConstant;
import org.example.common.context.UserContext;
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
import org.example.pojo.entity.Picture;
import org.example.pojo.entity.User;
import org.example.pojo.vo.PictureEntityVO;
import org.example.pojo.vo.PictureVO;
import org.example.pojo.vo.UserVO;
import org.example.server.service.CategoryService;
import org.example.server.service.PictureService;
import org.example.server.mapper.PictureMapper;
import org.example.server.service.UserService;
import org.example.common.util.RedisCacheUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Zou
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-05-14 21:47:55
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private AliOssUtil aliOssUtils;

    @Resource
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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    //程序化事务控制
    @Resource
    private TransactionTemplate transactionTemplate;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder()
                    .maximumSize(1000L)
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

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

        //校验用户是否允许上传图片
        user = userService.getById(user.getId());
        ThrowUtils.throwIf(!validAuth(user), ErrorCode.UPLOAD_NO_PERMISSION);
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
            //管理员上传图片或者用户上传图片至空间自动过审
            UserEnum userEnum = UserEnum.getByValue(user.getUserRole());
            if (UserEnum.ADMIN.equals(userEnum) || ObjUtil.isNotEmpty(space)) {
                picture.setReviewStatus(1);
                picture.setReviewMessage("管理员自动过审");
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
        String url = picture.getUrl();
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

        //构造redis Key
        String key = JSONUtil.toJsonStr(queryDTO);
        String hashKey = "queryPictureListUser:" + DigestUtils.md5DigestAsHex(key.getBytes());
        Long spaceId = queryDTO.getSpaceId();
        queryDTO.setNullSpaceId(!ObjUtil.isNotEmpty(spaceId));

        int ttlSeconds = 300 + RandomUtil.randomInt(0, 600);
        String cacheResult = redisCacheUtil.getWithLock(hashKey, ttlSeconds, () -> {
            QueryWrapper<Picture> queryWrapper = getQueryWrapper(queryDTO);
            Page<Picture> pictureList = this.page(new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize()), queryWrapper);
            List<Picture> pictures = pictureList.getRecords();

            Page<PictureVO> result = new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize(), pictureList.getTotal());
            if (ObjUtil.isEmpty(pictures)) {
                return null;
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

            return JSONUtil.toJsonStr(result);
        });

        if (cacheResult == null) {
            return new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
        }
        return JSONUtil.toBean(cacheResult, Page.class);
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

        return getPictureVO(picture);
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

        //审批成功后发送邮件通知上传者
        try {
            Long uploaderId = oldPicture.getUserId();
            if (uploaderId != null) {
                User uploader = userService.getById(uploaderId);
                if (uploader != null && StrUtil.isNotBlank(uploader.getUserEmail())) {
                    boolean passed = ReviewStatusEnum.PASS.getValue() == pictureReviewDTO.getReviewStatus();
                    emailUtil.sendReviewNotice(
                            uploader.getUserEmail(),
                            oldPicture.getName(),
                            passed,
                            pictureReviewDTO.getReviewMessage()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("审批通知邮件发送异常, pictureId={}, error={}", pictureReviewDTO.getId(), e.getMessage());
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
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索词不能为空");
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        //数据抓取
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        Element element = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjUtil.isEmpty(element), ErrorCode.OPERATION_ERROR, "获取元素失败");
        //获取图片元素
//       Elements imgElementList = element.select("img.mimg");
        //获取完整数据元素
        Elements imgElementList = element.select(".iusc");
        int uploadCount = 0;

        for (Element imgElement : imgElementList) {
            //String fileUrl = imgElement.attr("src");

//            if (StrUtil.isBlank(fileUrl)){
//                log.error("当前url为空，跳过 {}",fileUrl);
//                continue;
//            }
//           //处理图片上传地址，防止出现转义问题
//            int questionMarkIndex = fileUrl.indexOf("?");
//            if (questionMarkIndex > -1){
//                fileUrl = fileUrl.substring(0,questionMarkIndex);
//            }

            //获取data-m属性中的JSON字符串
            String dataM = imgElement.attr("m");
            String fileUrl;
            try {
                //解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                //获取murl字段(原始图片url)
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("图片url解析失败", e);
                continue;
            }

            if (StrUtil.isBlank(fileUrl)) {
                log.error("当前url为空，跳过 {}", fileUrl);
                continue;
            }

            //上传图片
            FileDTO fileDTO = new FileDTO();
            if (StrUtil.isNotBlank(namePrefix)) {
                fileDTO.setName(namePrefix + (uploadCount + 1));
            }
            if (ObjUtil.isNotEmpty(categoryId)) {
                fileDTO.setCategoryId(categoryId);
            }
            if (ObjUtil.isNotEmpty(tagList)) {
                String tags = JSONUtil.toJsonStr(tagList);
                fileDTO.setTags(tags);
            }
            try {
                Picture picture = this.upload(fileUrl, fileDTO);
                log.info("图片上传成功,id = {}", picture.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败，fileUrl = {}", fileUrl);
            }
            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }

    @Override
    public Page<PictureVO> queryPictureListUserCache(PictureQueryDTO queryDTO) {
        //构造本地缓存 Key
        String key = JSONUtil.toJsonStr(queryDTO);
        String hashKey = "queryPictureListUser:" + DigestUtils.md5DigestAsHex(key.getBytes());
        //本地缓存查询
        String cacheValue = LOCAL_CACHE.getIfPresent(hashKey);
        if (ObjUtil.isNotEmpty(cacheValue)) {
            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
            return cachePage;
        }

        //查找数据库数据
        Page<PictureVO> pictureVOPage = queryPictureListUser(queryDTO);
        //存入本地缓存
        cacheValue = JSONUtil.toJsonStr(pictureVOPage);

        LOCAL_CACHE.put(hashKey, cacheValue);

        //返回数据
        return pictureVOPage;
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
        Long id = pictureQueryDTO.getId();
        String name = pictureQueryDTO.getName();
        String introduction = pictureQueryDTO.getIntroduction();
        Long categoryId = pictureQueryDTO.getCategoryId();
        List<String> tags = pictureQueryDTO.getTags();
        Long picSize = pictureQueryDTO.getPicSize();
        Integer picWidth = pictureQueryDTO.getPicWidth();
        Integer picHeight = pictureQueryDTO.getPicHeight();
        Double picScale = pictureQueryDTO.getPicScale();
        Long userId = pictureQueryDTO.getUserId();
        Long spaceId = pictureQueryDTO.getSpaceId();
        Boolean nullSpaceId = pictureQueryDTO.getNullSpaceId();
        String searchText = pictureQueryDTO.getSearchText();
        //审核状态
        Integer reviewStatus = pictureQueryDTO.getReviewStatus();

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

        return queryWrapper;
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

    /**
     * 校验是否允许上传图片（必须绑定手机号和邮箱或者是管理员）
     */
    private boolean validAuth(User user) {
        //获取用户信息
        UserEnum userEnum = UserEnum.getByValue(user.getUserRole());
        String userPhone = user.getUserPhone();

        if (UserEnum.ADMIN.equals(userEnum)) {
            return true;
        }

        if (StrUtil.isBlank(userPhone)) {
            return false;
        }

        return true;
    }
}

