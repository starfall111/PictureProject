package org.example.pictureBacked;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Zou
 */
@SpringBootApplication
@MapperScan("org.example.pictureBacked.mapper")
public class PictureBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(PictureBackedApplication.class, args);
    }

}
