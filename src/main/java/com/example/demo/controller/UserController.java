package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.util.DBUtilUser;
import com.example.demo.util.LogUtil;
import com.influxdb.client.InfluxDBClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/web")
public class UserController {

    private final InfluxDBClient influxDBClient;

    public UserController(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @PostMapping("/login")
    public CommonResult<String> login(@RequestParam String username, @RequestParam String password) {
        CommonResult<String> loginResult = DBUtilUser.validateLogin(influxDBClient, username, password);

        // 记录登录操作日志
        if (loginResult.getCode() == 200) {
            LogUtil.logOperation(loginResult.getData(), "LOGIN", "User logged in successfully");
        } else {
            LogUtil.logOperation("N/A", "LOGIN", "Login failed for username: " + username);
        }

        return loginResult;
    }

    @PostMapping("/register")
    public CommonResult<String> register(@RequestParam String username, @RequestParam String password) {
        CommonResult<String> registerResult = DBUtilUser.registerUser(influxDBClient, username, password);

        // 记录注册操作日志
        if (registerResult.getCode() == 200) {
            LogUtil.logOperation(registerResult.getData(), "REGISTER", "User registered successfully");
        } else {
            LogUtil.logOperation("N/A", "REGISTER", "Registration failed for username: " + username);
        }

        return registerResult;
    }
}