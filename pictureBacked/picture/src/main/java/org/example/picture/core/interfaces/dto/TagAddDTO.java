package org.example.picture.core.interfaces.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Zou
 */
@Data
public class TagAddDTO implements Serializable {

    /**
     * 标签名称
     */
    private String name;

    private static final long serialVersionUID = 1L;
}
