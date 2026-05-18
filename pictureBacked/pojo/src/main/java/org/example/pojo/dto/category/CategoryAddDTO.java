package org.example.pojo.dto.category;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Zou
 */
@Data
public class CategoryAddDTO implements Serializable {

    /**
     * 分类名称
     */
    private String name;

    private static final long serialVersionUID = 1L;
}
