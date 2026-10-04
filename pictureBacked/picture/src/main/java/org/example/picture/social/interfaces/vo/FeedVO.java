package org.example.picture.social.interfaces.vo;

import org.example.picture.core.interfaces.vo.PictureSocialVO;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 动态 Feed 单条图片 VO
 *
 * @author Zou
 */
@Data
public class FeedVO implements Serializable {

    /**
     * 图片 ID
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片简介
     */
    private String introduction;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 分类 ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    // ==================== 发布者信息 ====================

    /**
     * 发布者用户 ID
     */
    private Long userId;

    /**
     * 发布者用户名
     */
    private String userName;

    /**
     * 发布者头像
     */
    private String userAvatar;

    // ==================== 社交统计（实时填充，不缓存） ====================

    /**
     * 社交信息（点赞数、收藏数等）
     */
    private PictureSocialVO socialInfo;

    // ==================== 动态标记 ====================

    /**
     * 是否为新动态（createTime > watermark）
     */
    private Boolean isNew;

    @Serial
    private static final long serialVersionUID = 1L;
}
