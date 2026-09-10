package org.example.picture.core.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author Zou
 * 图片信息修改（普通用户）
 */
@Data
public class PictureEditDTO implements Serializable {
    private Long id;
    private String name;
    private String introduction;
    private Long categoryId;
    private List<String> tags;

    @Serial
    private static final long serialVersionUID = 1L;

}
