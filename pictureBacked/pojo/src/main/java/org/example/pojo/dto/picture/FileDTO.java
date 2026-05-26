package org.example.pojo.dto.picture;

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
}
