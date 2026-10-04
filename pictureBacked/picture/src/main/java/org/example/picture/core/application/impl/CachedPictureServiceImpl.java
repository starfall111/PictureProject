package org.example.picture.core.application.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.identity.api.UserContext;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.util.RedisCacheUtil;
import org.example.picture.core.interfaces.dto.FileDTO;
import org.example.picture.core.interfaces.dto.PictureEditDTO;
import org.example.picture.core.interfaces.dto.PictureQueryDTO;
import org.example.picture.core.interfaces.dto.PictureReviewDTO;
import org.example.picture.core.interfaces.dto.PictureUpdateDTO;
import org.example.picture.core.interfaces.dto.PictureUploadByBatchDTO;
import org.example.picture.core.domain.model.Category;
import org.example.picture.core.domain.model.Picture;
import org.example.picture.space.domain.model.Space;
import org.example.identity.api.model.User;
import org.example.picture.core.interfaces.vo.PictureEntityVO;
import org.example.picture.core.interfaces.vo.PictureSocialVO;
import org.example.picture.core.interfaces.vo.PictureStatisticsVO;
import org.example.picture.core.interfaces.vo.PictureVO;
import org.example.identity.interfaces.vo.UserVO;
import org.example.shared.constants.RedisKeyConstants;
import org.example.picture.core.infrastructure.persistence.PictureMapper;
import org.example.picture.core.application.CategoryService;
import org.example.picture.core.application.PictureService;
import org.example.picture.social.application.SocialService;
import org.example.picture.space.application.SpaceService;
import org.example.identity.application.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存版图片服务实现
 * - 图片列表查询 → 热门查询缓存 + 普通查询 MD5 Key 缓存
 * - 图片详情页 → 缓存 PictureVO（不含 PictureSocialVO），走 getWithLock 防击穿
 * - 写操作 → 委托 dbPictureService + 后置缓存失效
 *
 * @author Zou
 */
@Slf4j
@Service("cachedPictureService")
public class CachedPictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheUtil redisCacheUtil;

    @Resource(name = "dbUserService")
    private UserService userService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private SpaceService spaceService;

    @Resource(name = "cachedSocialService")
    private SocialService socialService;

    @Resource
    @Qualifier("dbPictureService")
    private PictureService dbPictureService;

    /**
     * 用户查询图片列表
     *
     * @param queryDTO
     * @return
     */
    @Override
    public Page<PictureVO> queryPictureListUser(PictureQueryDTO queryDTO) {
        Long spaceId = queryDTO.getSpaceId();
        queryDTO.setNullSpaceId(!ObjUtil.isNotEmpty(spaceId));

        // 空间图片 → 不缓存（涉及权限校验）
        if (ObjUtil.isNotEmpty(spaceId)) {
            Page<PictureVO> result = doQueryPictureListUser(queryDTO);
            fillSocialData(result.getRecords());
            return result;
        }

        // 深度分页 → 不缓存
        if (queryDTO.getCurrent() > 10) {
            Page<PictureVO> result = doQueryPictureListUser(queryDTO);
            fillSocialData(result.getRecords());
            return result;
        }

        //判断是否为热门查询
        boolean isHot = isHotQuery(queryDTO);
        String md5Key = buildQueryMd5(queryDTO, isHot);

        String cacheKey;
        int ttlSeconds;
        if (isHot) {
            cacheKey = String.format(RedisKeyConstants.PIC_QUERY_HOT_KEY, md5Key);
            ttlSeconds = 300 + RandomUtil.randomInt(0, 600);
        } else {
            cacheKey = String.format(RedisKeyConstants.PIC_QUERY_NORMAL_KEY, md5Key);
            ttlSeconds = 300 + RandomUtil.randomInt(0, 120);
        }

        String json = redisCacheUtil.getWithLock(cacheKey, ttlSeconds, () -> {
            Page<PictureVO> result = doQueryPictureListUser(queryDTO);
            return JSONUtil.toJsonStr(result);
        });

        if (json == null) {
            return new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
        }
        // 手动反序列化，避免 Page<PictureVO> 泛型擦除导致 records 类型丢失
        JSONObject jsonObj = JSONUtil.parseObj(json);
        Page<PictureVO> result = new Page<>();
        result.setCurrent(jsonObj.getLong("current"));
        result.setSize(jsonObj.getLong("size"));
        result.setTotal(jsonObj.getLong("total"));
        JSONArray recordsArr = jsonObj.getJSONArray("records");
        if (recordsArr != null) {
            result.setRecords(recordsArr.toList(PictureVO.class));
        }
        fillSocialData(result.getRecords());
        return result;
    }

    /**
     * 用户查询图片详情
     *
     * @param id 图片ID
     * @return
     */
    @Override
    public PictureVO getByPictureIdUser(long id) {
        // 1. 查 DB 判断是否存在及是否空间图片
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR, "图片不存在");

        // 2. 空间图片 → 权限校验后直接返回（不缓存）
        if (ObjUtil.isNotEmpty(picture.getSpaceId())) {
            Space space = spaceService.getById(picture.getSpaceId());
            spaceService.validAuthUser(space, UserContext.get());
            PictureVO vo = getPictureVO(picture);
            return vo;
        }

        // 3. 公共图片 → getWithLock 缓存（防击穿）
        String detailKey = String.format(RedisKeyConstants.PIC_DETAIL_KEY, id);
        int ttl = 900 + RandomUtil.randomInt(0, 900);

        String json = redisCacheUtil.getWithLock(detailKey, ttl, () -> {
            PictureVO vo = getPictureVO(picture);
            PictureVO copy = new PictureVO();
            BeanUtil.copyProperties(vo, copy);
            copy.setSocialInfo(null);
            return JSONUtil.toJsonStr(copy);
        });

        PictureVO pictureVO = JSONUtil.toBean(json, PictureVO.class);

        // 4. 实时填充社交数据
        fillSocialData(Collections.singletonList(pictureVO));
        return pictureVO;
    }

    // ==================== 写操作：委托 dbPictureService + 后置缓存失效 ====================

    /**
     * 用户上传图片
     *
     * @param inputResource 上传文件资源
     * @param fileDTO       上传文件DTO
     * @return 上传的图片实体
     */
    @Override
    public Picture upload(Object inputResource, FileDTO fileDTO) throws Exception {
        Picture picture = dbPictureService.upload(inputResource, fileDTO);
        if (picture != null && picture.getId() != null) {
            invalidateDetailCache(picture.getId());
            invalidateAllQueryCache();
            invalidateUploadedListCache(picture.getUserId());
        }
        return picture;
    }

    /**
     * 用户下载图片
     *
     * @param picture 图片实体
     * @return 图片字节数组
     */
    @Override
    public byte[] download(Picture picture) throws FileNotFoundException, ClientException {
        return dbPictureService.download(picture);
    }

    /**
     * 管理员更新图片
     *
     * @param pictureUpdateDTO 更新图片DTO
     * @return 是否更新成功
     */
    @Override
    public boolean updatePicture(PictureUpdateDTO pictureUpdateDTO) {
        boolean result = dbPictureService.updatePicture(pictureUpdateDTO);
        if (result) {
            invalidateDetailCache(pictureUpdateDTO.getId());
            invalidateAllQueryCache();
            Picture picture = this.getById(pictureUpdateDTO.getId());
            if (picture != null) {
                invalidateUploadedListCache(picture.getUserId());
            }
        }
        return result;
    }

    /**
     * 用户编辑图片
     *
     * @param pictureEditDTO 编辑图片DTO
     * @return 是否编辑成功
     */
    @Override
    public boolean editPicture(PictureEditDTO pictureEditDTO) {
        boolean result = dbPictureService.editPicture(pictureEditDTO);
        if (result) {
            invalidateDetailCache(pictureEditDTO.getId());
            invalidateAllQueryCache();
            Picture picture = this.getById(pictureEditDTO.getId());
            if (picture != null) {
                invalidateUploadedListCache(picture.getUserId());
            }
        }
        return result;
    }

    /**
     * 用户删除图片
     *
     * @param id 图片ID
     * @return 是否删除成功
     */
    @Override
    public Boolean deletePicture(long id) throws Exception {
        Picture picture = this.getById(id);
        Boolean result = dbPictureService.deletePicture(id);
        if (Boolean.TRUE.equals(result)) {
            invalidateDetailCache(id);
            invalidateAllQueryCache();
            if (picture != null) {
                invalidateUploadedListCache(picture.getUserId());
            }
        }
        return result;
    }

    /**
     * 管理员查询图片列表
     *
     * @param queryDTO 查询图片列表DTO
     * @return 图片列表分页VO
     */
    @Override
    public Page<PictureEntityVO> queryPictureListAdmin(PictureQueryDTO queryDTO) {
        return dbPictureService.queryPictureListAdmin(queryDTO);
    }

    /**
     * 管理员根据图片ID查询图片详情
     *
     * @param id 图片ID
     * @return 图片实体
     */
    @Override
    public Picture getByPictureIdAdmin(long id) {
        return dbPictureService.getByPictureIdAdmin(id);
    }

    /**
     * 用户查询待审批图片列表
     *
     * @param queryDTO 查询待审批图片列表DTO
     * @return 待审批图片列表分页VO
     */
    @Override
    public Page<PictureVO> queryPendingPictures(PictureQueryDTO queryDTO) {
        // 待审批列表直接查 DB，不缓存
        return dbPictureService.queryPendingPictures(queryDTO);
    }

    /**
     * 用户自动审批待审批图片
     *
     * @param userId 用户ID
     * @return 审批成功图片数量
     */
    @Override
    public int autoApprovePicturesByBindPhone(Long userId) {
        int count = dbPictureService.autoApprovePicturesByBindPhone(userId);
        if (count > 0) {
            invalidateUploadedListCache(userId);
        }
        return count;
    }

    /**
     * 管理员审批待审批图片
     *
     * @param pictureReviewDTO 审批图片DTO
     * @return 是否审批成功
     */
    @Override
    public void pictureReview(PictureReviewDTO pictureReviewDTO) {
        dbPictureService.pictureReview(pictureReviewDTO);
        invalidateDetailCache(pictureReviewDTO.getId());
        invalidateAllQueryCache();
    }

    @Override
    public Integer pictureUploadByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO) {
        Integer count = dbPictureService.pictureUploadByBatch(pictureUploadByBatchDTO);
        if (count != null && count > 0) {
            invalidateAllQueryCache();
        }
        return count;
    }

    // ==================== 缓存失效方法 ====================

    /**
     * 删除图片详情缓存
     */
    public void invalidateDetailCache(Long pictureId) {
        String detailKey = String.format(RedisKeyConstants.PIC_DETAIL_KEY, pictureId);
        stringRedisTemplate.delete(detailKey);
    }

    /**
     * 删除所有图片查询缓存
     */
    public void invalidateAllQueryCache() {
        redisCacheUtil.deleteByPattern("pic:query:hot:*");
        redisCacheUtil.deleteByPattern("pic:query:normal:*");
    }

    /**
     * 删除用户上传列表缓存
     */
    private void invalidateUploadedListCache(Long userId) {
        if (userId != null) {
            redisCacheUtil.deleteByPattern(String.format("list:uploaded:%d:*", userId));
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 判断是否为热门查询
     */
    private boolean isHotQuery(PictureQueryDTO queryDTO) {
        // 首页推荐：无分类/标签/用户 + reviewStatus=1 + spaceId=null
        if (queryDTO.getCategoryId() == null
                && (queryDTO.getTags() == null || queryDTO.getTags().isEmpty())
                && queryDTO.getUserId() == null
                && ObjUtil.isNotEmpty(queryDTO.getReviewStatus())
                && queryDTO.getReviewStatus() == 1
                && !ObjUtil.isNotEmpty(queryDTO.getSpaceId())) {
            return true;
        }
        // 分类浏览：有 categoryId + reviewStatus=1
        if (queryDTO.getCategoryId() != null
                && ObjUtil.isNotEmpty(queryDTO.getReviewStatus())
                && queryDTO.getReviewStatus() == 1) {
            return true;
        }
        // 用户上传列表：有 userId + reviewStatus=1
        if (queryDTO.getUserId() != null
                && ObjUtil.isNotEmpty(queryDTO.getReviewStatus())
                && queryDTO.getReviewStatus() == 1) {
            return true;
        }
        return false;
    }

    /**
     * 构建查询 MD5 Key
     */
    private String buildQueryMd5(PictureQueryDTO queryDTO, boolean isHot) {
        List<String> tags = queryDTO.getTags();
        String tagsPart = "";
        if (tags != null && !tags.isEmpty()) {
            tagsPart = tags.stream().sorted().collect(Collectors.joining(","));
        }
        // 包含所有影响查询结果的参数，防止缓存串数据
        String raw = String.format(
                "cat=%s|" +
                        "search=%s|sf=%s|so=%s|" +
                        "cur=%d|ps=%d",
                queryDTO.getCategoryId(),
                truncate(queryDTO.getSearchText(), 100),
                queryDTO.getSortField(),
                queryDTO.getSortOrder(),
                queryDTO.getCurrent(),
                queryDTO.getPageSize());
        return DigestUtils.md5DigestAsHex(raw.getBytes());
    }

    /**
     * 截断过长字符串防止缓存 key 过大
     */
    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /**
     * 执行实际的图片列表查询（包含社交数据填充）
     */
    private Page<PictureVO> doQueryPictureListUser(PictureQueryDTO queryDTO) {
        // 复用 dbPictureService 的查询逻辑
        // 这里直接用 baseMapper 查询
        QueryWrapper<Picture> queryWrapper = PictureServiceImplHelper.getQueryWrapper(queryDTO);
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

        result = result.setRecords(pictureVOList);
        return result;
    }

    /**
     * 批量填充社交数据（仅公共图库图片）
     */
    private void fillSocialData(List<PictureVO> pictureVOList) {
        List<Long> publicIds = pictureVOList.stream()
                .filter(vo -> vo.getSpaceId() == null)
                .map(PictureVO::getId)
                .collect(Collectors.toList());
        if (publicIds.isEmpty()) {
            return;
        }

        User currentUser = UserContext.get();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        Map<Long, PictureStatisticsVO> statsMap = socialService.batchStatistics(publicIds);
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

    /**
     * 辅助类：构建查询条件（与 PictureServiceImpl.getQueryWrapper 相同逻辑）
     * 由于父类方法不可直接访问，提取为静态辅助方法
     */
    private static class PictureServiceImplHelper {
        static com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO) {
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
            Integer reviewStatus = pictureQueryDTO.getReviewStatus();
            Date startEditTime = pictureQueryDTO.getStartEditTime();
            Date endEditTime = pictureQueryDTO.getEndEditTime();

            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Picture> queryWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();

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
    }
}
