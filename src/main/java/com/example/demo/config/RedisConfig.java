package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisConfig {
    private String host;
    private int port;
    private String password;
    private int database;
    private long timeout;
    private long maxMemory = 2 * 1024 * 1024 * 1024L; // 2GB
}