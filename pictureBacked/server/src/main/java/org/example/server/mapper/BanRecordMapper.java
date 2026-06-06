package org.example.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.pojo.entity.BanRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * @author Zou
 * @description 针对表【ban_record(封禁记录)】的数据库操作Mapper
 * @Entity org.example.pojo.entity.BanRecord
 */
public interface BanRecordMapper extends BaseMapper<BanRecord> {

    /**
     * 查询用户的所有未解封记录
     */
    List<BanRecord> selectActiveByUserId(@Param("userId") Long userId);
}
