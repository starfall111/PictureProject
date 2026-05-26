package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收藏/取消收藏响应体
 */
@Data
public class ToggleFavoriteVO implements Serializable {

    /**
     * 是否已收藏
     */
    private Boolean favorited;

    /**
     * 收藏数
     */
    private Integer favoriteCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
