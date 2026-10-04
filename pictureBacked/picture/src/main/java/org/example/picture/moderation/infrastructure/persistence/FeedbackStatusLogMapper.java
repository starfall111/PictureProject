package org.example.picture.moderation.infrastructure.persistence;

import org.example.picture.moderation.domain.model.FeedbackStatusLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author Zou
 * @description 针对表【feedback_status_log(反馈状态变更日志)】的数据库操作Mapper
 * @Entity org.example.picture.moderation.domain.model.FeedbackStatusLog
 */
public interface FeedbackStatusLogMapper extends BaseMapper<FeedbackStatusLog> {

}
