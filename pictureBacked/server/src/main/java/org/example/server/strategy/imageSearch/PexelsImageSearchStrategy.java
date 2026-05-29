package org.example.server.strategy.imageSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.pexels.PexelsPicture;
import org.example.common.api.pexels.model.PexelsResponse;
import org.example.common.api.translate.BaiduTranslate;
import org.example.server.strategy.imageSearch.model.ImageSourceResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PexelsImageSearchStrategy implements ImageSearchStrategy {

    private final PexelsPicture pexelsPicture;
    private final BaiduTranslate baiduTranslate;

    @Override
    public String getSourceType() {
        return "pexels";
    }

    @Override
    public List<ImageSourceResult> searchImages(String searchText, int count) {
        List<PexelsResponse> responses = pexelsPicture.getPexelsPicture(searchText,count);

        return responses.stream()
                .limit(count)
                .map(this::toResult)
                .toList();
    }

    private ImageSourceResult toResult(PexelsResponse resp) {
        String introduction = String.format(
                "本图片来源于https://www.pexels.com\n由摄影师%s发布\n 摄影师主页:%s\n 图片原址:%s\n",
                resp.getPhotographer(),
                resp.getPhotographerUrl(),
                resp.getUrl()
        );
        // 将英文 alt 文本翻译为中文作为图片名称
        String chineseName = baiduTranslate.translateEnToZh(resp.getName());
        return new ImageSourceResult(resp.getOriginal(), chineseName, introduction);
    }
}
