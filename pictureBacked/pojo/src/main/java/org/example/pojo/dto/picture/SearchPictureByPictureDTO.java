package org.example.pojo.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureDTO implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
