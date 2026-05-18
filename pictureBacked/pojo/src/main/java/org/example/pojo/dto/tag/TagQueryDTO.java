package org.example.pojo.dto.tag;

import lombok.Data;
import org.example.pojo.PageRequest;

import java.io.Serializable;

/**
 * @author Zou
 */
@Data
public class TagQueryDTO extends PageRequest implements Serializable {

    /**
     * 标签名称（模糊搜索）
     */
    private String name;

    private static final long serialVersionUID = 1L;
}
