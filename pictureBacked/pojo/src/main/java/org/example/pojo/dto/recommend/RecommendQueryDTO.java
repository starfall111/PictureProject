package org.example.pojo.dto.recommend;

import lombok.Data;
import org.example.pojo.PageRequest;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 推荐查询请求 DTO
 */
@Data
public class RecommendQueryDTO extends PageRequest implements Serializable {

    /**
     * 推荐场景：homepage / guess / similar
     */
    private String scene = "homepage";

    /**
     * 基准图片 ID（similar 场景使用）
     */
    private Long basePictureId;

    /**
     * 需要排除的图片 ID 列表
     */
    private List<Long> excludeIds;

    /**
     * 分类 ID（similar 场景筛选）
     */
    private Long categoryId;

    /**
     * 推荐分页上限 50
     */
    @Override
    public int getPageSize() {
        return Math.min(Math.max(super.getPageSize(), 1), 50);
    }

    @Serial
    private static final long serialVersionUID = 1L;
}
