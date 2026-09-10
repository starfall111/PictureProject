package org.example.picture.core.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class FileDTO {
    private Long id;

    private String fileUrl;

    private String name;

    private Long categoryId;

    /**
     * 空间 id
     */
    private Long spaceId;

    private String tags;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 上传用户 id（异步场景使用，HTTP 场景由 UserContext 自动填充）
     */
    private Long userId;
}
