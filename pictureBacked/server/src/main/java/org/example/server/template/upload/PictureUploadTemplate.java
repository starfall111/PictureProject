package org.example.server.template.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.AliOssUtil;
import org.example.pojo.dto.picture.UploadPictureDTO;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private AliOssUtil aliOssUtil;

    public final UploadPictureDTO upload(Object inputResource) throws Exception {
        //校验文件是否合规
        String fileSuffix = validPicture(inputResource);

        ThrowUtils.throwIf(StrUtil.isBlank(fileSuffix),ErrorCode.PARAMS_ERROR,"图片格式错误");

        //从URL提取文件名和后缀
        String fileName = getOriginFileName(inputResource);
        //文件名为空时抛出错误
        ThrowUtils.throwIf(StrUtil.isBlank(fileName),ErrorCode.PARAMS_ERROR,"文件名不能为空");
        //确保文件名包含后缀
        if (!fileName.endsWith("." + fileSuffix)) {
            fileName = fileName + "." + fileSuffix;
        }

        //下载文件至临时文件
        File tempFile = null;
        String url = null;
        try {
            //创建临时目录
            //创建临时目录（使用项目根目录下的temp文件夹）
            String projectRoot = System.getProperty("user.dir");
            File tempDir = new File(projectRoot, "temp");
            //创建临时文件（前缀最多3字符，后缀为图片格式）
            tempFile = File.createTempFile("pic", "." + fileSuffix, tempDir);

            getTempFile(inputResource,tempFile);


            url = aliOssUtil.upload(FileUtil.readBytes(tempFile), fileName);

            //OSS中存放三份文件 1.缩略图 2.压缩图 3.原始图像
            //数据库内也维护三份图片URL 1.缩略图 URL 2. 压缩图URL 3. 原始图片URL
            String format = "image/format,webp";
            String resize = "image/resize,s_180";

            long fileSize = FileUtil.size(tempFile);

            String originImageUrl = url;

            url = aliOssUtil.processPicture(format,originImageUrl);
            String thumbnailImageUrl = aliOssUtil.processPicture(resize,originImageUrl);

            //获取图片信息：宽度、高度、宽高比
            BufferedImage bufferedImage = ImageIO.read(tempFile);
            ThrowUtils.throwIf(ObjUtil.isEmpty(bufferedImage), ErrorCode.SYSTEM_ERROR);

            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            double scale = (double) width / height;

            UploadPictureDTO uploadPictureDTO = new UploadPictureDTO();
            uploadPictureDTO.setName(fileName);
            uploadPictureDTO.setUrl(url);
            uploadPictureDTO.setThumbnailUrl(thumbnailImageUrl);
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
        } finally {
            //清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
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
     * 获取临时文件字节流
     * */
    protected  abstract void getTempFile(Object inputResource, File file) throws IOException;
}
