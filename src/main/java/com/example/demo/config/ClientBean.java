package com.example.demo.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientBean {

    @Bean
    @ConfigurationProperties(prefix = "influxdb")
    public InfluxDBProperties influxDBProperties() {
        return new InfluxDBProperties();
    }

    @Bean
    public InfluxDBClient influxDBClient(InfluxDBProperties properties) {
        return InfluxDBClientFactory.create(
                properties.getUrl(),
                properties.getToken().toCharArray(),
                properties.getOrg(),
                properties.getBucket()
        );
    }

    @Data
    public static class InfluxDBProperties {
        private String url;
        private String token;
        private String org;
        private String bucket;
    }
}
