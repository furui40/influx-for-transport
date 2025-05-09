package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.service.UserService;
import com.example.demo.utils.LogUtil;
import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/web")
public class UserController {

    private final InfluxDBClient influxDBClient;
    private final UserService userService;



    @PostMapping("/login")
    public CommonResult<String> login(@RequestParam String username, @RequestParam String password) {
        CommonResult<String> loginResult = userService.validateLogin(influxDBClient, username, password);

        if (loginResult.getCode() == 200) {
            String[] parts = loginResult.getData().split("::");
            String userId = parts[0];
            String status = parts.length > 1 ? parts[1] : "normal";

            LogUtil.logOperation(userId, "LOGIN", "User logged in successfully");

            return CommonResult.success(userId + "::" + status);
        } else {
            LogUtil.logOperation("N/A", "LOGIN", "Login failed for username: " + username);
            return loginResult;
        }
    }


    @PostMapping("/register")
    public CommonResult<String> register(@RequestParam String username, @RequestParam String password) {
        CommonResult<String> registerResult = userService.registerUser(influxDBClient, username, password);

        // 记录注册操作日志
        if (registerResult.getCode() == 200) {
            LogUtil.logOperation(registerResult.getData(), "REGISTER", "User registered successfully");
        } else {
            LogUtil.logOperation("N/A", "REGISTER", "Registration failed for username: " + username);
        }

        return registerResult;
    }

    @PostMapping("/modifypassword")
    public CommonResult<String> modifyPassword(@RequestParam String useId, @RequestParam String password,@RequestParam String newpassword) {
        CommonResult<String> modifyResult = userService.modifyPassword(influxDBClient, useId, password,newpassword);

        // 记录注册操作日志
        if (modifyResult.getCode() == 200) {
            LogUtil.logOperation(modifyResult.getData(), "MODIFY", "Modify Password successfully");
        } else {
            LogUtil.logOperation(useId, "MODIFY", modifyResult.getMessage());
        }

        return modifyResult;
    }
}