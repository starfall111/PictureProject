package org.example.pojo.dto.social;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分享记录请求体
 */
@Data
public class ShareRecordDTO implements Serializable {

    /**
     * 分享平台：wechat/weibo/qq
     */
    private String platform;

    @Serial
    private static final long serialVersionUID = 1L;
}
