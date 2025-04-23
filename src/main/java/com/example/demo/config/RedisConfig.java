package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisConfig {
    private String host;
    private int port;
    private String password;
    private int database;
    private long timeout;
    private long maxMemory = 2 * 1024 * 1024 * 1024L; // 2GB

    // 上传记录Redis配置
    private UploadRecordConfig uploadRecord;

    @Data
    public static class UploadRecordConfig {
        private int database = 1; // 默认使用1号数据库
        private long expireTime = 2592000L; // 默认30天过期(秒)
    }

    // 新增高频传感器配置
    private HighSensorConfig highSensor;

    @Data
    public static class HighSensorConfig {
        private int database = 2;
        private long expireTime = 2592000L; // 30天过期
    }
}