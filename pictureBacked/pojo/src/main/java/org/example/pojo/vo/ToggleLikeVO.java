package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 点赞/取消点赞响应体
 */
@Data
public class ToggleLikeVO implements Serializable {

    /**
     * 是否已点赞
     */
    private Boolean liked;

    /**
     * 点赞数
     */
    private Integer likeCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
