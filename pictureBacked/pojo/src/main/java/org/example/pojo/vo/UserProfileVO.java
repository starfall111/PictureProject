package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户档案 VO
 */
@Data
public class UserProfileVO implements Serializable {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 上传图片数量
     */
    private Integer uploadCount;

    /**
     * 总点赞数
     */
    private Integer totalLikes;

    /**
     * 总收藏数
     */
    private Integer totalFavorites;

    /**
     * 总浏览数
     */
    private Integer totalViews;

    /**
     * 总分享数
     */
    private Integer totalShares;

    /**
     * 总下载次数
     */
    private Integer totalDownloads;

    /**
     * 用户点赞的图片数量
     */
    private Integer userLikeCount;

    /**
     * 用户收藏的图片数量
     */
    private Integer userFavoriteCount;

    /**
     * 分类列表
     */
    private List<CategoryBriefVO> categories;

    /**
     * 关注数（正在关注多少人）
     */
    private Integer followCount;

    /**
     * 粉丝数（被多少人关注）
     */
    private Integer followerCount;

    /**
     * 当前登录用户是否关注了该用户
     */
    private Boolean isFollowed;

    @Serial
    private static final long serialVersionUID = 1L;
}
