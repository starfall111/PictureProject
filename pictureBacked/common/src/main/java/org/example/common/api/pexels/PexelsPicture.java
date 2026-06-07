package org.example.common.api.pexels;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.aliyun.core.http.HttpHeader;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.example.common.api.pexels.model.PexelsResponse;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PexelsPicture {

    @Value("${pexels.api-key}")
    private String apiKey;

    //精选图片
    private final String curatedApiUrl = "https://api.pexels.com/v1/curated?per_page=1&per_page=%s";

    //搜索图片
    private final String searchApiUrl = "https://api.pexels.com/v1/search?query=%s&per_page=1&per_page=%s";

    public List<PexelsResponse> getPexelsPicture(String query,int count) {
        String requestUrl = String.format(curatedApiUrl,count);

        if (StrUtil.isNotBlank(query)) {
            requestUrl = String.format(searchApiUrl, query,count);
        }

        try {
            //获取响应体
            HttpResponse response = HttpRequest.get(requestUrl)
                    .header("Authorization", apiKey)
                    .timeout(10000)
                    .execute();

            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            //转换为项目中适配的实体类
            String responseBody = response.body();
            Map<String, Object> resultMap = JSONUtil.toBean(responseBody, Map.class);

            List<Map<String, Object>> photos = (List<Map<String, Object>>) resultMap.get("photos");

            List<PexelsResponse> result = new ArrayList<>();

            for (Map<String, Object> photo : photos) {
                PexelsResponse pexelsResponse = new PexelsResponse();
                Map<String, Object> src = (Map<String, Object>) photo.get("src");

                Integer width = (Integer) photo.get("width");
                Integer height = (Integer) photo.get("height");
                String url = (String) photo.get("url");
                String photographer = (String) photo.get("photographer");
                String photographerUrl = (String) photo.get("photographer_url");
                String name = (String) photo.get("alt");
                String original = (String) src.get("original");

                pexelsResponse.setWidth(width);
                pexelsResponse.setHeight(height);
                pexelsResponse.setUrl(url);
                pexelsResponse.setPhotographer(photographer);
                pexelsResponse.setPhotographerUrl(photographerUrl);
                pexelsResponse.setName(name);
                pexelsResponse.setOriginal(original);

                result.add(pexelsResponse);
            }

            return result;

        } catch (Exception e) {
            log.error("Pexels 请求失败，", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

    }
}
