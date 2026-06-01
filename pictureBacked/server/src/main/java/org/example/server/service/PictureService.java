package org.example.server.service;

import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.picture.*;
import org.example.pojo.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.vo.PictureEntityVO;
import org.example.pojo.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
* @author Zou
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-05-14 21:47:55
*/
public interface PictureService extends IService<Picture> {
    /**
    * 上传图片文件
    * @param inputResource 文件 或 url
    * @param fileDTO 入库的图片文件请求体
    * */
    Picture upload(Object inputResource,FileDTO fileDTO) throws Exception;

    /**
    * 下载文件
     * @param picture 图片
    * */
    byte[] download(Picture picture) throws ClientException, FileNotFoundException;

    //1.图片编辑信息：更改图片信息（管理员/普通用户）
    boolean updatePicture(PictureUpdateDTO pictureUpdateDTO);

    boolean editPicture(PictureEditDTO pictureEditDTO);

    //2.删除图片
    Boolean deletePicture(long id) throws Exception;
    //3.分页查询图片（管理员/普通用户）
    Page<PictureEntityVO> queryPictureListAdmin(PictureQueryDTO queryDTO);

    Page<PictureVO> queryPictureListUser(PictureQueryDTO queryDTO);
    //4.根据id获取图片信息（管理员/普通用户）
    Picture getByPictureIdAdmin(long id);

    PictureVO getByPictureIdUser(long id);

    //管理员对用户上传的图片进行审批：三种状态 0待审批（当用户在上传图片和修改图片时都需要将status重置，管理员传图时进行自动过审） 1 审批通过 2 审批未通过 ；为防止误操作，三种状态可互相流转
    void pictureReview(PictureReviewDTO pictureReviewDTO);
    /**
     * todo 针对人工审核图片过于麻烦的问题，以下解决方案
     *  1.项目智能化升级，后续项目开发完毕后添加智能模块：
     *  智能客服：可直接查询当前用户图片未审核通过的图片、识别用户意图搜索图片（FunctionCalling）；添加 QA 问答系统（RAG）系统
     *  智能审批系统：对接OCR技术来实现制动审批功能
     *  2.分级策略
     *  后续可开放vip或安全用户，针对这些用户不进行审批
     *  3.手机号强制绑定（当前已实现）
     *  当用户想要上传图片时，如果账号未绑定手机号，直接拒绝用户上传图片
     *   4.举报机制
     * */

    //管理员批量获取图片
    Integer pictureUploadByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO);

    /**
     * 查询当前用户待审批的图片列表（直接查 DB）
     */
    Page<PictureVO> queryPendingPictures(PictureQueryDTO queryDTO);

    /**
     * 绑定手机号后自动过审最新 100 张待审批图片
     *
     * @param userId 用户ID
     * @return 过审的图片数量
     */
    int autoApprovePicturesByBindPhone(Long userId);

}
