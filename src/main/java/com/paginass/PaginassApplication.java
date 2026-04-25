package com.paginass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PaginassApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaginassApplication.class, args);
    }
}
