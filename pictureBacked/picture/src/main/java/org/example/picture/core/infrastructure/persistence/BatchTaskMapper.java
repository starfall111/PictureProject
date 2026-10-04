package org.example.picture.core.infrastructure.persistence;

import org.example.picture.core.domain.model.BatchTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author Zou
 * @description 针对表【batch_task(批量获取图片任务)】的数据库操作Mapper
 */
public interface BatchTaskMapper extends BaseMapper<BatchTask> {

}
