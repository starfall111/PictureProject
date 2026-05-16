package org.example.server.service;

import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.pojo.DeleteRequest;
import org.example.pojo.dto.picture.PictureEditDTO;
import org.example.pojo.dto.picture.PictureQueryDTO;
import org.example.pojo.dto.picture.PictureUpdateDTO;
import org.example.pojo.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
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
    * @param file 文件
    * @param imageId 入库的图片文件Id
    * */
    Picture upload(MultipartFile file,Long imageId) throws Exception;

    /**
    * 下载文件
     * @param picture 图片
    * */
    byte[] download(Picture picture) throws ClientException, FileNotFoundException;

    //1.图片编辑信息：更改图片信息（管理员/普通用户）
    boolean updatePicture(PictureUpdateDTO pictureUpdateDTO);


    boolean editPicture(PictureEditDTO pictureEditDTO);

    //2.删除图片
    boolean deletePicture(long id) throws Exception;
    //3.分页查询图片（管理员/普通用户）
    public Page<Picture> queryPictureListAdmin(PictureQueryDTO queryDTO);

    public Page<PictureVO> queryPictureListUser(PictureQueryDTO queryDTO);
    //4.根据id获取图片信息（管理员/普通用户）
    public Picture getByPictureIdAdmin(long id);

    public PictureVO getByPictureIdUser(long id);
}
