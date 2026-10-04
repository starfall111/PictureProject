package org.example.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动入口 — 统一装配所有限界上下文模块
 *
 * @author Zou
 */

@SpringBootApplication
@MapperScan({
        "org.example.identity.infrastructure.persistence",
        "org.example.notification.infrastructure.persistence",
        "org.example.picture.core.infrastructure.persistence",
        "org.example.picture.social.infrastructure.persistence",
        "org.example.picture.moderation.infrastructure.persistence",
        "org.example.picture.space.infrastructure.persistence",
        "org.example.marketing.infrastructure.persistence"
})
@ComponentScan("org.example")
@EnableScheduling
@EnableAsync
public class PictureBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureBackedApplication.class, args);
    }

}
