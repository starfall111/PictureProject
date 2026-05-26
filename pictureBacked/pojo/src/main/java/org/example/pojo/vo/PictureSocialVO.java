package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片社交信息响应体
 *
 * @author Zou
 */
@Data
public class PictureSocialVO implements Serializable {

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 收藏数
     */
    private Integer favoriteCount;

    /**
     * 分享数
     */
    private Integer shareCount;

    /**
     * 当前用户是否已点赞
     */
    private Boolean isLiked;

    /**
     * 当前用户是否已收藏
     */
    private Boolean isFavorited;

    @Serial
    private static final long serialVersionUID = 1L;
}
