package org.example.common.api.pexels.model;

import lombok.Data;

@Data
public class PexelsResponse {
    //图片宽
    private Integer width;

    //图片高
    private Integer height;

    //图片网址(原网址，拼接简介)
    private String url;

    //摄影师名称(后续用来拼接简介)
    private String photographer;

    //摄影师在Pexels的主页(拼接简介)
    private String photographerUrl;

    //原始图像url
    private String original;

    //图像名称
    private String name;
}
