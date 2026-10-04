package org.example.picture.core.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class PictureUploadByBatchDTO {

    private String searchText;
    private Integer count = 10;
    private String profile;
    private List<String> tags;
    private Long categoryId;
    /** 图片搜索来源，默认 pexels */
    private String searchSource = "pexels";
    /** 目标空间ID（可选） */
    private Long spaceId;
}
