package org.example.picture.core.domain.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 图片联表统计查询结果载体
 * picture LEFT JOIN picture_statistics LEFT JOIN category
 */
@Data
public class PictureWithStats implements Serializable {

    private Long id;
    private String name;
    private String url;
    private String thumbnailUrl;
    private Integer picWidth;
    private Integer picHeight;
    private Long categoryId;
    private String categoryName;
    private Long spaceId;
    private Long userId;
    private String tags;
    private Integer reviewStatus;
    private Date createTime;

    private Integer likeCount;
    private Integer favoriteCount;
    private Integer shareCount;
    private Integer viewCount;
    private Integer downloadCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
