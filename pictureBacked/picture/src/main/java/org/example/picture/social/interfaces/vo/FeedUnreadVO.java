package org.example.picture.social.interfaces.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 动态未读数 VO
 *
 * @author Zou
 */
@Data
public class FeedUnreadVO implements Serializable {

    /**
     * 未读动态数
     */
    private Integer unreadCount;

    @Serial
    private static final long serialVersionUID = 1L;
}
