package com.smartreply.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the SmartReply AI demo.
 * Spring Boot wires the controllers, services and strategy beans together
 * (dependency injection); every email/reply/keyword rule itself is plain Java.
 */
@SpringBootApplication
public class SmartReplyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartReplyApplication.class, args);
    }
}
