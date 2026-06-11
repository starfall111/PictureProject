package org.example.server.strategy.imageSearch;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.server.strategy.imageSearch.model.ImageSourceResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BingImageSearchStrategy implements ImageSearchStrategy {

    @Override
    public String getSourceType() {
        return "bing";
    }

    @Override
    public List<ImageSourceResult> searchImages(String searchText, int count) {
        List<ImageSourceResult> results = new ArrayList<>();

        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        Element element = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(element)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }

        Elements imgElementList = element.select(".iusc");

        int index = 1;
        for (Element imgElement : imgElementList) {
            if (results.size() >= count) {
                break;
            }

            String dataM = imgElement.attr("m");
            try {
                String fileUrl = JSONUtil.parseObj(dataM).getStr("murl");
                if (StrUtil.isNotBlank(fileUrl)) {
                    String imageName = searchText + index++;
                    results.add(new ImageSourceResult(fileUrl, imageName, null));
                }
            } catch (Exception e) {
                log.error("图片url解析失败", e);
            }
        }

        return results;
    }
}
