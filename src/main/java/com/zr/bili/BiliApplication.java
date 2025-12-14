package com.zr.bili;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zr.bili.mapper")
public class BiliApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiliApplication.class, args);
    }

}
