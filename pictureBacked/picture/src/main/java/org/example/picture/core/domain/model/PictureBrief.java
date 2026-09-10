package org.example.picture.core.domain.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 图片简要信息实体（用于 SQL 联表查询结果映射）
 */
@Data
public class PictureBrief implements Serializable {

    private Long id;

    private String name;

    private String url;

    private String thumbnailUrl;

    private Integer picWidth;

    private Integer picHeight;

    private String categoryName;

    /**
     * 图片上传者 id
     */
    private Long userId;

    /**
     * 标签（JSON 字符串，数据库原始值）
     */
    private String tags;

    private Date createTime;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer viewCount;

    private Integer downloadCount;

    /**
     * 点赞时间（仅点赞列表）
     */
    private Date likeTime;

    /**
     * 收藏时间（仅收藏列表）
     */
    private Date favoriteTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
