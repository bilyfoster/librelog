package com.onelpro.librelog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LibrelogApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibrelogApplication.class, args);
    }
}
