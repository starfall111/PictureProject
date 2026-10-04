package org.example.picture.moderation.infrastructure.persistence;

import org.example.picture.moderation.domain.model.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author Zou
 * @description 针对表【feedback(反馈)】的数据库操作Mapper
 * @Entity org.example.picture.moderation.domain.model.Feedback
 */
public interface FeedbackMapper extends BaseMapper<Feedback> {

}
