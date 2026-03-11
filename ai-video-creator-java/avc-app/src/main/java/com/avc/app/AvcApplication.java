package com.avc.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.avc")
@MapperScan("com.avc.infra.mapper")
public class AvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvcApplication.class, args);
    }
}
