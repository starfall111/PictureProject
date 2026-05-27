package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片简要信息 VO（用于列表展示）
 */
@Data
public class PictureBriefVO implements Serializable {

    /**
     * 图片 id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 收藏数
     */
    private Integer favoriteCount;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 下载数
     */
    private Integer downloadCount;

    /**
     * 点赞时间（仅点赞列表返回）
     */
    private Date likeTime;

    /**
     * 收藏时间（仅收藏列表返回）
     */
    private Date favoriteTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
