package com.meem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MeemBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeemBeApplication.class, args);
    }

}
