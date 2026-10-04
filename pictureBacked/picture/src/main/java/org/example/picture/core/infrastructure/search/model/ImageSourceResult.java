package org.example.picture.core.infrastructure.search.model;

import lombok.Data;

/**
 * 图片搜索策略的统一响应体
 */
@Data
public class ImageSourceResult {

    /**
     * 图片下载地址
     */
    private String url;

    /**
     * 图片名称（可选）
     */
    private String name;

    /**
     * 图片简介（可选）
     */
    private String introduction;

    public ImageSourceResult(String url) {
        this.url = url;
    }

    public ImageSourceResult(String url, String name, String introduction) {
        this.url = url;
        this.name = name;
        this.introduction = introduction;
    }
}
