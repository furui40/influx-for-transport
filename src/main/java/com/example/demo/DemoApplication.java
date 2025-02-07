package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		// 启动 Spring Boot 应用
		ApplicationContext context = SpringApplication.run(DemoApplication.class, args);

		// 获取 Test0 Bean 并执行测试
		Test0 test0 = context.getBean(Test0.class);
		test0.testInfluxDB();
	}
}
