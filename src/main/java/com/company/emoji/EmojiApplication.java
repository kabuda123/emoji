package com.company.emoji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EmojiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmojiApplication.class, args);
    }
}