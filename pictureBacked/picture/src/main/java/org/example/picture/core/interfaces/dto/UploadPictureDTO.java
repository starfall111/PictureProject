package org.example.picture.core.interfaces.dto;

import lombok.Data;

@Data
public class UploadPictureDTO {
    private String name;
    private String url;
    private String thumbnailUrl;
    private String originUrl;
    private Long picSize;
    private Integer picWidth;
    private Integer picHeight;
    private Double picScale;
    private String picFormat;
}
