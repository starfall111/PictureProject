package org.example.picture.social.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户图片列表查询请求体（点赞/收藏列表）
 */
@Data
public class UserPictureQueryDTO implements Serializable {

    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 分类筛选（可选）
     */
    private String category;

    /**
     * 标签筛选（可选，OR 关系）
     */
    private List<String> tags;

    /**
     * 排序方式：time=时间倒序（默认），hot=热度
     */
    private String sortBy = "time";

    @Serial
    private static final long serialVersionUID = 1L;

    public Integer getPageSize() {
        return Math.min(pageSize != null ? pageSize : 10, 50);
    }

    public Integer getCurrent() {
        return current != null && current > 0 ? current : 1;
    }

    public String getSortBy() {
        return "hot".equals(sortBy) ? "hot" : "time";
    }
}
