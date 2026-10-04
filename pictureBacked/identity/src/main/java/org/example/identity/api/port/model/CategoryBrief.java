package org.example.identity.api.port.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类简要信息（发布语言：身份 ← 图片）
 *
 * @author Zou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBrief {

    private Long id;

    private String name;
}
