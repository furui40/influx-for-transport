package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 允许所有路径
                .allowedOrigins("http://localhost:5173")
//                .allowedOrigins("http://192.168.43.36:8081")
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // 允许的方法
                .allowCredentials(true)  // 允许发送凭证
                .maxAge(3600);  // 预检请求缓存时间
    }
}
