package org.example.server.service.event;

import lombok.extern.slf4j.Slf4j;
import org.example.server.service.PictureService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 绑定手机号事件监听器 — 异步触发自动过审
 *
 * @author Zou
 */
@Slf4j
@Component
public class BindPhoneEventListener {

    @Resource(name = "dbPictureService")
    private PictureService pictureService;

    @Async
    @EventListener
    public void handleBindPhoneEvent(BindPhoneEvent event) {
        try {
            int count = pictureService.autoApprovePicturesByBindPhone(event.getUserId());
            log.info("绑定手机号自动过审完成：userId={}, approved={}", event.getUserId(), count);
        } catch (Exception e) {
            log.error("绑定手机号自动过审失败：userId={}", event.getUserId(), e);
        }
    }
}
