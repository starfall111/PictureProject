package org.example.identity.api.port.model;

import lombok.Data;

import java.util.List;

/**
 * 用户内容统计结果（发布语言：身份 ← 图片）
 *
 * @author Zou
 */
@Data
public class UserContentStats {

    /** 用户点赞的图片数量 */
    private Integer userLikeCount;

    /** 用户收藏的图片数量 */
    private Integer userFavoriteCount;

    /** 公共图库上传数量 */
    private Integer uploadCount;

    /** 获赞总数 */
    private Integer totalLikes;

    /** 被收藏总数 */
    private Integer totalFavorites;

    /** 浏览总数 */
    private Integer totalViews;

    /** 分享总数 */
    private Integer totalShares;

    /** 下载总数 */
    private Integer totalDownloads;

    /** 用户图片涉及的分类 */
    private List<CategoryBrief> categories;
}
