package org.example.common.api.imagesearch;

import lombok.extern.slf4j.Slf4j;
import org.example.common.api.imagesearch.model.ImageSearchResult;
import org.example.common.api.imagesearch.sub.GetImageFirstUrlApi;
import org.example.common.api.imagesearch.sub.GetImageListApi;
import org.example.common.api.imagesearch.sub.GetImagePageUrlApi;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://zjx-project.oss-cn-beijing.aliyuncs.com/2026/05/be6b54f9-903e-49d4-b928-5f3a88a09c90.webp";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
