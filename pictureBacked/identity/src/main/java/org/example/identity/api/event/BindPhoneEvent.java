package org.example.identity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 绑定手机号事件 — 触发自动过审
 *
 * @author Zou
 */
@Getter
public class BindPhoneEvent extends ApplicationEvent {

    /**
     * 绑定手机号的用户ID
     */
    private final Long userId;

    public BindPhoneEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }
}
