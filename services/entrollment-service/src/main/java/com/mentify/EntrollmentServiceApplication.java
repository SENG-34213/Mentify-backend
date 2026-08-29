package com.mentify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class EntrollmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EntrollmentServiceApplication.class, args);
    }
}
