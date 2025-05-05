package com.example.demo.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import lombok.Data;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ClientBean {

    @Bean
    @ConfigurationProperties(prefix = "influxdb")
    public InfluxDBProperties influxDBProperties() {
        return new InfluxDBProperties();
    }

    @Bean
    public InfluxDBClient influxDBClient(InfluxDBProperties properties) {
        // 创建自定义的OkHttpClient.Builder并设置超时
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(90))
                .readTimeout(Duration.ofSeconds(90))
                .writeTimeout(Duration.ofSeconds(90));

        // 创建InfluxDB客户端选项
        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(properties.getUrl())
                .authenticateToken(properties.getToken().toCharArray())
                .org(properties.getOrg())
                .bucket(properties.getBucket())
                .okHttpClient(okHttpBuilder)  // 设置自定义的OkHttpClient配置
                .build();

        return InfluxDBClientFactory.create(options);
    }

    @Data
    public static class InfluxDBProperties {
        private String url;
        private String token;
        private String org;
        private String bucket;
    }
}