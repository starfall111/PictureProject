package org.example.server.template.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import org.example.common.constants.PictureConstant;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Zou
 */
@Service
public class FileUploadPicture extends PictureUploadTemplate{
    @Override
    protected String validPicture(Object inputResource) {
        MultipartFile file = (MultipartFile) inputResource;
        //校验文件是否为空
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        //校验文件大小，最大为5M
        long fileSize = file.getSize();
        ThrowUtils.throwIf(fileSize > PictureConstant.MAX_IMAGE_SIZE,ErrorCode.PARAMS_ERROR,"文件大小不能超过5M");
        //校验文件格式
        String  fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
        //文件格式判断
        ThrowUtils.throwIf(!ArrayUtil.contains(PictureConstant.IMAGE_TYPE_LIST, fileSuffix), ErrorCode.PARAMS_ERROR, "不支持的图片格式");

        return fileSuffix;
    }

    @Override
    protected String getOriginFileName(Object inputResource) {
        MultipartFile file = (MultipartFile) inputResource;

        return file.getOriginalFilename();
    }

    @Override
    protected void getTempFile(Object inputResource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputResource;

        multipartFile.transferTo(file);
    }
}
