package org.example.pojo.dto.feed;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 动态时间线查询请求 DTO
 *
 * @author Zou
 */
@Data
public class FeedQueryDTO implements Serializable {

    /**
     * 当前页码（从 1 开始）
     */
    private Integer current = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    @Serial
    private static final long serialVersionUID = 1L;
}
