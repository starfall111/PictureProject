package org.example.picture.moderation.infrastructure.persistence;

import org.example.picture.moderation.domain.model.FeedbackReply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author Zou
 * @description 针对表【feedback_reply(反馈回复)】的数据库操作Mapper
 * @Entity org.example.picture.moderation.domain.model.FeedbackReply
 */
public interface FeedbackReplyMapper extends BaseMapper<FeedbackReply> {

}
