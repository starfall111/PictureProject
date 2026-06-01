package org.example.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 推荐结果 VO
 */
@Data
public class RecommendVO implements Serializable {

    /**
     * 推荐图片列表（与普通图片列表查询返回一致的 PictureVO）
     */
    private List<PictureVO> pictures;

    /**
     * 推荐理由（如 "热度推荐"、"猜你喜欢"、"相似图片"）
     */
    private String reason;

    /**
     * 推荐场景
     */
    private String scene;

    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;

    @Serial
    private static final long serialVersionUID = 1L;
}
