package org.example.pojo.vo.feedback;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 反馈回复 VO
 *
 * @author Zou
 */
@Data
public class FeedbackReplyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 回复 ID
     */
    private Long id;

    /**
     * 回复用户 ID
     */
    private Long userId;

    /**
     * 回复用户名
     */
    private String userName;

    /**
     * 回复用户头像
     */
    private String userAvatar;

    /**
     * 回复类型
     */
    private String replyType;

    /**
     * 回复类型描述
     */
    private String replyTypeDesc;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;
}
