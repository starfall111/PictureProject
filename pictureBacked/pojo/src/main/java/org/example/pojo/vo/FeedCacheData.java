package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 动态缓存内部数据对象
 * <p>
 * 将动态首页数据、未读数、最新时间和总数打包缓存，
 * 保证未读数和时间线来自同一份数据源，天然一致。
 *
 * @author Zou
 */
@Data
public class FeedCacheData implements Serializable {

    /**
     * 首页数据（缓存固定条数，如 20 条）
     */
    private List<FeedVO> items;

    /**
     * 未读动态数（从 DB COUNT 计算得出）
     */
    private Integer unreadCount;

    /**
     * 最新一条动态的 createTime 毫秒时间戳（用于 markRead 时更新水位线）
     */
    private Long latestTime;

    /**
     * 关注用户动态总数
     */
    private Long total;

    @Serial
    private static final long serialVersionUID = 1L;
}
