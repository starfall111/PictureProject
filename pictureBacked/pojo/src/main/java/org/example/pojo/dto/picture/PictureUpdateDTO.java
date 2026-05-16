package org.example.pojo.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author Zou
 * 图片更新操作（管理员）
 */
@Data
public class PictureUpdateDTO implements Serializable {
    private Long id;
    private String name;
    private String introduction;
    private String category;
    private List<String> tags;

    @Serial
    private static final long serialVersionUID = 1L;
}
