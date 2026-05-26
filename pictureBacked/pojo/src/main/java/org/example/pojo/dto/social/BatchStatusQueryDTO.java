package org.example.pojo.dto.social;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量状态查询请求体（点赞/收藏状态）
 */
@Data
public class BatchStatusQueryDTO implements Serializable {

    /**
     * 图片 id 列表
     */
    private List<Long> pictureIds;

    @Serial
    private static final long serialVersionUID = 1L;
}
