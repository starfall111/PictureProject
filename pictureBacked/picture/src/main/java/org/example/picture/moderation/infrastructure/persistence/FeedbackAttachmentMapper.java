package org.example.picture.moderation.infrastructure.persistence;

import org.example.picture.moderation.domain.model.FeedbackAttachment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author Zou
 * @description 针对表【feedback_attachment(反馈附件)】的数据库操作Mapper
 * @Entity org.example.picture.moderation.domain.model.FeedbackAttachment
 */
public interface FeedbackAttachmentMapper extends BaseMapper<FeedbackAttachment> {

}
