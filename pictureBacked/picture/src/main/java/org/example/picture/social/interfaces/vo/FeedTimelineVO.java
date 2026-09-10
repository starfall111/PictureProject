package org.example.picture.social.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 动态时间线分页结果 VO
 * <p>
 * unreadCount 与 records 来自同一份缓存数据，保证一致性
 *
 * @author Zou
 */
@Data
public class FeedTimelineVO implements Serializable {

    /**
     * 动态列表
     */
    private List<FeedVO> records;

    /**
     * 总数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页数量
     */
    private Long size;

    /**
     * 未读动态数（从同一缓存派生，保证与 records 一致）
     */
    private Integer unreadCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
