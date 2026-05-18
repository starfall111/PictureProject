package org.example.common.template.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.PictureConstant;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.util.AliOssUtil;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@Slf4j
public abstract class FileUploadTemplate {

    @Resource
    private AliOssUtil aliOssUtil;

    public final String upload(Object inputResource) throws Exception {
        //校验文件是否合规
        validPicture(inputResource);

        //从URL提取文件名和后缀
        //todo 获取原始文件名
        String fileName = getOriginFileName(inputResource);
        String fileSuffix = FileUtil.getSuffix(fileName);
        //后缀为空抛出错误
        ThrowUtils.throwIf(StrUtil.isBlank(fileSuffix),ErrorCode.PARAMS_ERROR,"图片格式错误");
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
            File tempDir = new File(PictureConstant.TEMP_FILE_URL);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            //创建临时文件（前缀最多3字符，后缀为图片格式）
            tempFile = File.createTempFile("pic", "." + fileSuffix, tempDir);
//            //下载URL内容到临时文件
//            HttpUtil.downloadFile(inputResource, tempFile);
//
//            //读取临时文件字节，上传到阿里云OSS
//            byte[] fileBytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
            byte[] fileBytes = getTempFileByte(inputResource,tempFile);
            url = aliOssUtil.upload(fileBytes, fileName);

            //获取图片信息：宽度、高度、宽高比
            BufferedImage bufferedImage = ImageIO.read(tempFile);
            ThrowUtils.throwIf(ObjUtil.isEmpty(bufferedImage), ErrorCode.SYSTEM_ERROR);

            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            double scale = (double) width / height;

            return url;
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
    protected abstract void validPicture(Object inputResource);

    /**
     * 获取原始文件名
     * */
    protected  abstract String getOriginFileName(Object inputResource);

    /**
     * 获取临时文件字节流
     * */
    protected  abstract byte[] getTempFileByte(Object inputResource,File file);
}
