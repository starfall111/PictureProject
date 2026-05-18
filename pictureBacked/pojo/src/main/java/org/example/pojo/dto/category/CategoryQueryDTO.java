package org.example.pojo.dto.category;

import lombok.Data;
import org.example.pojo.PageRequest;

import java.io.Serializable;

/**
 * @author Zou
 */
@Data
public class CategoryQueryDTO extends PageRequest implements Serializable {

    /**
     * 分类名称（模糊搜索）
     */
    private String name;

    private static final long serialVersionUID = 1L;
}
