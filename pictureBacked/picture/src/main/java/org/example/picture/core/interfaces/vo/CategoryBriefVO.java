package org.example.picture.core.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分类简要信息 VO
 */
@Data
public class CategoryBriefVO implements Serializable {

    /**
     * 分类 ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    @Serial
    private static final long serialVersionUID = 1L;
}
