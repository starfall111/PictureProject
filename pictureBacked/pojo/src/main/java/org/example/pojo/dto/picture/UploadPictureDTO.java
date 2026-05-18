package org.example.pojo.dto.picture;

import lombok.Data;

@Data
public class UploadPictureDTO {
    private String name;
    private String url;
    private Long picSize;
    private Integer picWidth;
    private Integer picHeight;
    private Double picScale;
    private String picFormat;
}
