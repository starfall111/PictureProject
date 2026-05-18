package org.example.pojo.dto.picture;

import lombok.Data;

import java.util.List;

@Data
public class FileDTO {
    private Long id;

    private String fileUrl;

    private String name;

    private Long categoryId;

    private String tags;
}
