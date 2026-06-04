package org.example.server.service;

import org.example.pojo.dto.feed.FeedQueryDTO;
import org.example.pojo.vo.FeedTimelineVO;
import org.example.pojo.vo.FeedUnreadVO;

/**
 * 动态（Feed Timeline）服务接口
 * <p>
 * 提供关注用户的图片动态时间线、未读数统计和已读标记功能
 *
 * @author Zou
 */
public interface FeedService {

    /**
     * 获取动态时间线
     *
     * @param userId   当前登录用户 ID
     * @param queryDTO 分页查询参数
     * @return 动态分页结果（含 unreadCount）
     */
    FeedTimelineVO getTimeline(Long userId, FeedQueryDTO queryDTO);

    /**
     * 获取未读动态数
     *
     * @param userId 当前登录用户 ID
     * @return 未读数信息
     */
    FeedUnreadVO getUnreadCount(Long userId);

    /**
     * 标记动态已读（更新水位线并清除缓存）
     *
     * @param userId 当前登录用户 ID
     */
    void markRead(Long userId);
}
