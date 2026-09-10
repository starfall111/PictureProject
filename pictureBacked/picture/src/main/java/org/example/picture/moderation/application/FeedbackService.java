package org.example.picture.moderation.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.picture.moderation.interfaces.dto.*;
import org.example.picture.moderation.domain.model.Feedback;
import org.example.picture.moderation.interfaces.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 反馈服务接口
 *
 * @author Zou
 */
public interface FeedbackService extends IService<Feedback> {

    /** 上传反馈附件 */
    FeedbackAttachmentVO uploadAttachment(MultipartFile file);

    /** 提交反馈 */
    long submitFeedback(FeedbackSubmitDTO dto);

    /** 撤回反馈 */
    void withdrawFeedback(Long feedbackId);

    /** 个人反馈列表 */
    Page<FeedbackListItemVO> getMyFeedbackList(FeedbackQueryDTO dto);

    /** 反馈详情 */
    FeedbackVO getFeedbackDetail(Long feedbackId);

    /** 追加回复 */
    void replyFeedback(Long feedbackId, FeedbackReplyDTO dto);

    /** 重新打开 */
    void reopenFeedback(Long feedbackId);

    /** 确认解决 */
    void confirmFeedback(Long feedbackId);

    /** 个人反馈统计 */
    FeedbackStatsVO getMyFeedbackStats();

    /** 管理端反馈列表 */
    Page<FeedbackListItemVO> getAdminFeedbackList(FeedbackQueryDTO dto);

    /** 管理端反馈详情 */
    FeedbackVO getAdminFeedbackDetail(Long feedbackId);

    /** 认领反馈 */
    void claimFeedback(Long feedbackId);

    /** 转派反馈 */
    void transferFeedback(Long feedbackId, Long targetHandlerId);

    /** 管理员回复 */
    void adminReplyFeedback(Long feedbackId, FeedbackReplyDTO dto);

    /** 内部备注 */
    void addInternalNote(Long feedbackId, FeedbackReplyDTO dto);

    /** 拒绝反馈 */
    void rejectFeedback(Long feedbackId, String reason);

    /** 调整优先级 */
    void updatePriority(Long feedbackId, String priority);

    /** 反馈转举报 */
    long convertToReport(Long feedbackId, FeedbackConvertDTO dto);

    /** 批量关闭 */
    void batchCloseFeedback(List<Long> feedbackIds, String reason);

    /** 管理端统计 */
    FeedbackStatsVO getAdminFeedbackStats();
}
