package org.example.server.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.bytebuddy.implementation.bytecode.Throw;
import org.example.common.annotation.CheckAuth;
import org.example.common.constants.UserConstant;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.common.util.AliOssUtil;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.picture.PictureEditDTO;
import org.example.pojo.dto.picture.PictureQueryDTO;
import org.example.pojo.dto.picture.PictureUpdateDTO;
import org.example.pojo.entity.Picture;
import org.example.pojo.vo.PictureVO;
import org.example.server.service.PictureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/file")
public class PictureController {

    private static final Logger log = LoggerFactory.getLogger(PictureController.class);
    @Resource
    private PictureService pictureService;

    @Resource
    private AliOssUtil aliOssUtils;

    /**
     * 图片上传
     * */
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @PostMapping("/upload")
    public BaseResponse<PictureVO> upload(
            @RequestParam("files") MultipartFile file,
            @RequestParam(value = "id", required = false) Long imageId
    ) throws Exception {
        //文件判空
        ThrowUtils.throwIf(ObjUtil.isEmpty(file), ErrorCode.PARAMS_ERROR, "文件不能为空");

        Picture picture = pictureService.upload(file, imageId);

        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);

        return ResultUtils.success(pictureVO);
    }

    /**
     * 图片下载
     * */
    @GetMapping("/download")
    public void download(Long id, HttpServletResponse response) throws IOException, ClientException {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(picture),ErrorCode.PARAMS_ERROR);
        try{
            //字节流
            byte[] result = pictureService.download(picture);
            String fileName = picture.getName();
            //设置响应头
            response.setContentType("application/octet-stream:charset=UTF-8");
            response.setHeader("Content-Disposition","attachment;filename=" + fileName);
            //写入响应
            response.getOutputStream().write(result);
            response.getOutputStream().flush();
        }catch (Exception e){
            log.error("file download error, filename is " + picture.getName() );
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

    }

    //1.图片编辑信息：更改图片信息（管理员/普通用户）
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @PostMapping("/picture/update")
    public BaseResponse<Boolean> updatePicture(PictureUpdateDTO pictureUpdateDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureUpdateDTO),ErrorCode.PARAMS_ERROR);

        boolean result = pictureService.updatePicture(pictureUpdateDTO);

        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);
    }

    @PostMapping("/picture/edit")
    public BaseResponse<Boolean> editPicture(PictureEditDTO pictureEditDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureEditDTO),ErrorCode.PARAMS_ERROR);

        boolean result = pictureService.editPicture(pictureEditDTO);

        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);
    }
    //2.删除图片
    @DeleteMapping("/picture/delete")
    public BaseResponse<Boolean> deletePicture(DeleteRequest deleteRequest) throws Exception {
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest),ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);

        boolean result = pictureService.deletePicture(id);

        ThrowUtils.throwIf(!result,ErrorCode.SYSTEM_ERROR);

        return ResultUtils.success(result);

    }
    //3.分页查询图片（管理员/普通用户）
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @PostMapping("/picture/admin/query")
    public BaseResponse<Page<Picture>> queryPictureAdmin(PictureQueryDTO pictureQueryDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO),ErrorCode.PARAMS_ERROR);

        Page<Picture> result = pictureService.queryPictureListAdmin(pictureQueryDTO);

        return ResultUtils.success(result);
    }

    @PostMapping("/picture/user/query")
    public BaseResponse<Page<PictureVO>> queryPictureUser(PictureQueryDTO pictureQueryDTO){
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureQueryDTO),ErrorCode.PARAMS_ERROR);

        Page<PictureVO> result = pictureService.queryPictureListUser(pictureQueryDTO);

        return ResultUtils.success(result);
    }

    //4.根据id获取图片信息（管理员/普通用户）
    @CheckAuth(mustRole = UserConstant.ADMIN_AUTH_ROLE)
    @GetMapping("/picture/admin/{id}")
    public BaseResponse<Picture> getPictureByIdAdmin(@PathVariable Long id){
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.getByPictureIdAdmin(id);

        return ResultUtils.success(picture);
    }

    @GetMapping("/picture/user/{id}")
    public BaseResponse<PictureVO> getPictureByIdUser(@PathVariable Long id){
        ThrowUtils.throwIf(ObjUtil.isEmpty(id),ErrorCode.PARAMS_ERROR);

        PictureVO pictureVO = pictureService.getByPictureIdUser(id);

        return ResultUtils.success(pictureVO);
    }
}
