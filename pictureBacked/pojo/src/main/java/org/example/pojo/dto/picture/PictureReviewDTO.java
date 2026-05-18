package org.example.pojo.dto.picture;

import lombok.Data;

import java.util.Date;

@Data
public class PictureReviewDTO {
    private long id;
    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

}
