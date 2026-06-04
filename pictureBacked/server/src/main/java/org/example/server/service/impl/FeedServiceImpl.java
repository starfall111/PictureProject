package org.example.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.pojo.dto.feed.FeedQueryDTO;
import org.example.pojo.entity.Category;
import org.example.pojo.entity.Picture;
import org.example.pojo.entity.User;
import org.example.pojo.vo.FeedTimelineVO;
import org.example.pojo.vo.FeedUnreadVO;
import org.example.pojo.vo.FeedVO;
import org.example.pojo.vo.PictureSocialVO;
import org.example.pojo.vo.PictureStatisticsVO;
import org.example.server.mapper.PictureMapper;
import org.example.server.mapper.UserFollowMapper;
import org.example.server.mapper.UserMapper;
import org.example.server.service.CategoryService;
import org.example.server.service.FeedService;
import org.example.server.service.SocialService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态 Feed 服务 DB 实现（不含缓存）
 * <p>
 * 直接查询数据库，负责：
 * 1. 查 user_follow 获取关注列表
 * 2. 查 picture 获取关注用户的已审核公开图片
 * 3. COUNT(createTime > watermark) -> unreadCount
 * 4. 排序: ORDER BY create_time DESC
 *
 * @author Zou
 */
@Slf4j
@Service("dbFeedService")
public class FeedServiceImpl implements FeedService {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private UserFollowMapper userFollowMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CategoryService categoryService;

    @Resource(name = "cachedSocialService")
    private SocialService cachedSocialService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public FeedTimelineVO getTimeline(Long userId, FeedQueryDTO queryDTO) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(queryDTO == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");

        int current = queryDTO.getCurrent();
        int pageSize = queryDTO.getPageSize();
        ThrowUtils.throwIf(current < 1, ErrorCode.PARAMS_ERROR, "页码不合法");
        ThrowUtils.throwIf(pageSize < 1 || pageSize > 50, ErrorCode.PARAMS_ERROR, "每页数量不合法");

        // 分页查询关注用户的动态图片
        Page<Picture> page = new Page<>(current, pageSize);
        Page<Picture> resultPage = pictureMapper.selectFeedPictures(page, userId);

        // 转换为 FeedVO 列表
        List<FeedVO> feedVOList = resultPage.getRecords().stream()
                .map(this::pictureToFeedVO)
                .collect(Collectors.toList());

        // 批量填充用户信息
        fillUserInfo(feedVOList);

        // 批量填充分类名称
        fillCategoryName(feedVOList);

        // 实时填充社交数据
        fillSocialData(feedVOList);

        // 组装返回结果
        FeedTimelineVO vo = new FeedTimelineVO();
        vo.setRecords(feedVOList);
        vo.setTotal(resultPage.getTotal());
        vo.setCurrent(resultPage.getCurrent());
        vo.setSize(resultPage.getSize());
        // DB 层不计算 unreadCount，由缓存层补充
        vo.setUnreadCount(0);
        return vo;
    }

    @Override
    public FeedUnreadVO getUnreadCount(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");

        Long watermark = Long.valueOf(Objects.requireNonNull(stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstants.FEED_WATERMARK_KEY, userId))));

        Date date = new Date(watermark);
        // 默认水位线为 0（表示所有动态都是未读）
        Long unreadCount = pictureMapper.countFeedUnread(userId, date);

        FeedUnreadVO vo = new FeedUnreadVO();
        vo.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
        return vo;
    }

    @Override
    public void markRead(Long userId) {
        // DB 层无需操作，水位线由缓存层管理
    }

    // ==================== 私有方法 ====================

    /**
     * Picture 实体 -> FeedVO
     */
    private FeedVO pictureToFeedVO(Picture picture) {
        FeedVO vo = new FeedVO();
        BeanUtil.copyProperties(picture, vo);
        vo.setId(picture.getId());
        // tags: JSON 字符串 -> List<String>
        if (picture.getTags() != null) {
            vo.setTags(JSONUtil.toList(picture.getTags(), String.class));
        }
        // 默认非新动态，由缓存层标记
        vo.setIsNew(false);
        return vo;
    }

    /**
     * 批量填充发布者用户信息
     */
    private void fillUserInfo(List<FeedVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream()
                .map(FeedVO::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        for (FeedVO vo : records) {
            User user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
        }
    }

    /**
     * 批量填充分类名称
     */
    private void fillCategoryName(List<FeedVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> categoryIds = records.stream()
                .map(FeedVO::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return;
        }
        Map<Long, Category> categoryMap = categoryService.listByIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));
        for (FeedVO vo : records) {
            if (vo.getCategoryId() != null && categoryMap.containsKey(vo.getCategoryId())) {
                vo.setCategoryName(categoryMap.get(vo.getCategoryId()).getName());
            }
        }
    }

    /**
     * 实时填充社交数据（点赞数、收藏数等）
     */
    private void fillSocialData(List<FeedVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> pictureIds = records.stream()
                .map(FeedVO::getId)
                .collect(Collectors.toList());
        if (pictureIds.isEmpty()) {
            return;
        }
        Map<Long, PictureStatisticsVO> statsMap = cachedSocialService.batchStatistics(pictureIds);
        for (FeedVO vo : records) {
            PictureStatisticsVO stats = statsMap.getOrDefault(vo.getId(), new PictureStatisticsVO());
            PictureSocialVO socialVO = new PictureSocialVO();
            socialVO.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
            socialVO.setFavoriteCount(stats.getFavoriteCount() != null ? stats.getFavoriteCount() : 0);
            socialVO.setShareCount(stats.getShareCount() != null ? stats.getShareCount() : 0);
            socialVO.setViewCount(stats.getViewCount() != null ? stats.getViewCount() : 0);
            socialVO.setDownloadCount(stats.getDownloadCount() != null ? stats.getDownloadCount() : 0);
            vo.setSocialInfo(socialVO);
        }
    }
}
