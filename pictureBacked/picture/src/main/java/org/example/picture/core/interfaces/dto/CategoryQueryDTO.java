package org.example.picture.core.interfaces.dto;

import lombok.Data;
import org.example.shared.contract.PageRequest;

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
