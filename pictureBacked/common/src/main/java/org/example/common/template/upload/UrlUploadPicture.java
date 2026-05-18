package org.example.common.template.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import org.example.common.constants.PictureConstant;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * @author Zou
 */
@Service
public class UrlUploadPicture extends PictureUploadTemplate {

    @Override
    protected String validPicture(Object inputResource) {
        String filePath = (String) inputResource;
        //校验url不能为空
        ThrowUtils.throwIf(StrUtil.isBlank(filePath), ErrorCode.PARAMS_ERROR, "图片地址不能为空");
        //校验url格式，必须为http或则https开头
        try {
            new URL(filePath);
        } catch (MalformedURLException error) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        ThrowUtils.throwIf(!filePath.startsWith("https://") && !filePath.startsWith("http://"), ErrorCode.PARAMS_ERROR, "仅支持HTTP和HTTPS协议的图片地址");
        //发送head请求得到文件格式和文件大小
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, filePath).execute();
            //未正常返回，直接跳出（部分网站不支持head请求，跳出校验进行下一步）
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return "";
            }
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //校验文件大小
            String contentLength = response.header("Content-Length");
            try {
                if (StrUtil.isNotBlank(contentLength)) {
                    long fileSize = Long.valueOf(contentLength);
                    ThrowUtils.throwIf(fileSize > PictureConstant.MAX_IMAGE_SIZE, ErrorCode.PARAMS_ERROR, "图片大小不能超过5M");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小格式错误");
            }

            switch (contentType){
                case "image/jpeg":
                    return "jpeg";
                case "image/jpg":
                    return "jpg";
                case "image/png":
                    return "png";
                case "image/webp":
                    return "webp";
                default:
                    return "";
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        //
    }

    @Override
    protected String getOriginFileName(Object inputResource) {
        String filePath = (String) inputResource;
        return FileUtil.getName(filePath);
    }

    @Override
    protected void getTempFile(Object inputResource, File file) throws IOException {
        String filePath = (String) inputResource;
        //下载URL内容到临时文件
        HttpUtil.downloadFile(filePath, file);
    }
}
