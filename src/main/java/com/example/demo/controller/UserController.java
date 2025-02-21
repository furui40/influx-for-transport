package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.util.DBUtilUser;
import com.influxdb.client.InfluxDBClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/web")
public class UserController {

    private final InfluxDBClient influxDBClient;

    // 注入 InfluxDBClient
    public UserController(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }
    // 登录接口
    @PostMapping("/login")
    public CommonResult<String> login(@RequestParam String username, @RequestParam String password) {
        // 使用已注入的单例客户端
        CommonResult<String> loginResult = DBUtilUser.validateLogin(influxDBClient, username, password);
        return loginResult;
    }

    @PostMapping("/register")
    public CommonResult<String> register(@RequestParam String username, @RequestParam String password) {
        // 使用已注入的单例客户端
        CommonResult<String> loginResult = DBUtilUser.registerUser(influxDBClient, username, password);
        return loginResult;
    }

}

