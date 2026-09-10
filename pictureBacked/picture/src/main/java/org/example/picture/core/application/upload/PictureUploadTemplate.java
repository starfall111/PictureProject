package org.example.picture.core.application.upload;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.shared.exception.BusinessException;
import org.example.shared.exception.ErrorCode;
import org.example.shared.exception.ThrowUtils;
import org.example.shared.util.AliOssUtil;
import org.example.picture.core.interfaces.dto.UploadPictureDTO;

import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private AliOssUtil aliOssUtil;

    public UploadPictureDTO upload(Object inputResource) throws Exception {
        //校验文件是否合规
        String fileSuffix = validPicture(inputResource);

        ThrowUtils.throwIf(StrUtil.isBlank(fileSuffix), ErrorCode.PARAMS_ERROR, "图片格式错误");

        //从URL提取文件名和后缀
        String fileName = getOriginFileName(inputResource);
        //文件名为空时抛出错误
        ThrowUtils.throwIf(StrUtil.isBlank(fileName), ErrorCode.PARAMS_ERROR, "文件名不能为空");
        //确保文件名包含后缀
        if (!fileName.endsWith("." + fileSuffix)) {
            fileName = fileName + "." + fileSuffix;
        }

        // 一次性获取图片字节（内存中，无磁盘 I/O）
        byte[] imageBytes = getImageBytes(inputResource);
        long fileSize = imageBytes.length;

        String url = null;
        try {
            // 直接上传 byte[] 到 OSS（无中间文件）
            url = aliOssUtil.upload(imageBytes, fileName);
            String originImageUrl = url;

            // 并行执行 OSS 图片处理
            String format = "image/format,webp";
            String resize = "image/resize,s_180";

            CompletableFuture<String> compressedFuture = CompletableFuture.supplyAsync(() -> {
                try { return aliOssUtil.processPicture(format, originImageUrl); }
                catch (Exception e) { throw new CompletionException(e); }
            });
            CompletableFuture<String> thumbnailFuture = CompletableFuture.supplyAsync(() -> {
                try { return aliOssUtil.processPicture(resize, originImageUrl); }
                catch (Exception e) { throw new CompletionException(e); }
            });

            // 获取图片尺寸（从内存 byte[]，无磁盘读取）
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            ThrowUtils.throwIf(ObjUtil.isEmpty(bufferedImage), ErrorCode.SYSTEM_ERROR);
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            double scale = (double) width / height;

            // 等待两个处理完成
            String compressedUrl = compressedFuture.join();
            String thumbnailUrl = thumbnailFuture.join();

            UploadPictureDTO uploadPictureDTO = new UploadPictureDTO();
            uploadPictureDTO.setName(fileName);
            uploadPictureDTO.setUrl(compressedUrl);
            uploadPictureDTO.setThumbnailUrl(thumbnailUrl);
            uploadPictureDTO.setOriginUrl(originImageUrl);
            uploadPictureDTO.setPicFormat(fileSuffix);
            uploadPictureDTO.setPicSize(fileSize);
            uploadPictureDTO.setPicHeight(height);
            uploadPictureDTO.setPicWidth(width);
            uploadPictureDTO.setPicScale(scale);
            return uploadPictureDTO;
        } catch (Exception e) {
            if (url != null) {
                aliOssUtil.deleteByUrl(url);
            }
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
    }

    /**
     * 校验数据源
     * */
    protected abstract String validPicture(Object inputResource);

    /**
     * 获取原始文件名
     * */
    protected  abstract String getOriginFileName(Object inputResource);

    /**
     * 获取图片字节数据
     * */
    protected abstract byte[] getImageBytes(Object inputResource) throws IOException;
}
