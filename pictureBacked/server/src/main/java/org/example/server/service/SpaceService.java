package org.example.server.service;

import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.pojo.dto.space.SpaceAddDTO;
import org.example.pojo.dto.space.SpaceEditDTO;
import org.example.pojo.dto.space.SpaceQueryDTO;
import org.example.pojo.dto.space.SpaceUpdateDTO;
import org.example.pojo.entity.Space;
import org.example.pojo.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.pojo.entity.User;
import org.example.pojo.vo.SpaceVO;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * @author Zou
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2026-05-21 20:57:25
 */
public interface SpaceService extends IService<Space> {

    //新增空间操作
    long addSpace(SpaceAddDTO spaceAddDTO);

    //1.空间编辑信息：更改空间信息（管理员/普通用户）
    boolean updateSpace(SpaceUpdateDTO spaceUpdateDTO);

    boolean editSpace(SpaceEditDTO spaceEditDTO);

    //2.删除空间
    boolean deleteSpace(long id) throws Exception;

    //3.分页查询空间（管理员）
    Page<Space> querySpaceListAdmin(SpaceQueryDTO queryDTO);

    //4.根据id获取空间信息（普通用户）
    List<SpaceVO> getSpaceByUserId(long id);

    //5.根据Id返回空间详细信息
    Space getSpaceById(long id);

    //更严格的空间权限校验，只有本人才可以操作
    void validAuthUser(Space space, User user);
}
