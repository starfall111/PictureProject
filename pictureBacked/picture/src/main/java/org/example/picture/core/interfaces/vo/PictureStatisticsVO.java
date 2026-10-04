package org.example.picture.core.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片统计数据响应体
 */
@Data
public class PictureStatisticsVO implements Serializable {

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
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
