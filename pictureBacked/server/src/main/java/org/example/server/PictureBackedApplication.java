package org.example.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Zou
 */

@SpringBootApplication
@MapperScan("org.example.server.mapper")
@ComponentScan({"org.example.server","org.example.common"})
@EnableScheduling
@EnableAsync
public class PictureBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureBackedApplication.class, args);
    }

}
