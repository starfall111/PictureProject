package org.example.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.pojo.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Date;
import java.util.List;

/**
* @author Zou
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2026-05-11 20:54:16
* @Entity entity.User
*/
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按条件筛选用户ID（系统消息定向发送）
     */
    List<Long> selectIdsByFilter(@Param("filterRole") String filterRole,
                                 @Param("filterSpaceLevel") Integer filterSpaceLevel,
                                 @Param("filterRegisterStart") Date filterRegisterStart,
                                 @Param("filterRegisterEnd") Date filterRegisterEnd);
}




