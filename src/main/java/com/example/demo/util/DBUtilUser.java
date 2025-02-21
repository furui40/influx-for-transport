package com.example.demo.util;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.User;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DBUtilUser {

    // 注册方法
    public static CommonResult<String> registerUser(InfluxDBClient client, String userName, String password) {
        // 创建查询语句，检查用户名是否已存在
        if(userName.length() == 0 || password.length() == 0){
            System.out.println("Username is null.");
            return CommonResult.failed("用户名或密码为空");
        }
        QueryApi queryApi = client.getQueryApi();
        String fluxQuery = "from(bucket: \"test2\") " +
                "|> range(start: 0) " +
                "|> filter(fn: (r) => r._measurement == \"user_data\") " +
                "|> filter(fn: (r) => r._field == \"user_name\" and r._value == \"" + userName + "\") ";

        System.out.println(fluxQuery);
        // 执行查询，查看是否有重复用户名
        List<User> existingUsers = queryApi.query(fluxQuery, User.class);

        // 如果查询结果不为空，说明用户名已存在
        if (!existingUsers.isEmpty()) {
            System.out.println("Username already exists.");
            return CommonResult.failed("用户名已存在");
        }

        // 使用当前时间戳作为 userId
        String userId = String.valueOf(Instant.now().toEpochMilli());

        // 创建一个用户实例
        User newUser = new User()
                .setUserId(userId)
                .setUserName(userName)
                .setPassword(password)
                .setUserStatus("active");  // 默认设置为 active 状态

        // 创建并写入 Point 数据
        Point point = Point.measurement("user_data")
                .addTag("user_id", newUser.getUserId())
                .addField("user_name", newUser.getUserName())
                .addField("password", newUser.getPassword())
                .addField("user_status", newUser.getUserStatus())
                .time(Instant.now(), WritePrecision.NS);

        // 写入数据库
        client.getWriteApiBlocking().writePoint("test2", "test", point);

        // 返回注册成功的提示信息及userId
        return CommonResult.success(newUser.getUserId());
    }

    // 登录验证方法
    public static CommonResult<String> validateLogin(InfluxDBClient client, String userName, String password) {
        // 创建查询语句，检查用户名、密码是否匹配且用户状态为 active
        if(userName.length() == 0 || password.length() == 0){
            System.out.println("Username is null.");
            return CommonResult.failed("用户名或密码为空");
        }
        QueryApi queryApi = client.getQueryApi();
        String fluxQuery1 = "from(bucket: \"test2\") " +
                "|> range(start: 0) " +
                "|> filter(fn: (r) => r._measurement == \"user_data\") " +
                "|> filter(fn: (r) => r._field == \"user_name\" and r._value == \"" + userName + "\") ";

        // 执行查询
        List<User> users = queryApi.query(fluxQuery1, User.class);

        // 判断是否找到符合条件的用户
        if (users.isEmpty()) {
            System.out.println("Invalid username");
            return CommonResult.failed("用户名不存在");
        }

        // 如果存在符合条件的用户，返回 user_id
        String userId = users.get(0).getUserId();

        String fluxQuery2 = "from(bucket: \"test2\") " +
                "|> range(start: 0) " +
                "|> filter(fn: (r) => r._measurement == \"user_data\") " +
                "|> filter(fn: (r) => r.user_id == \"" + userId + "\") ";

        String temp_password = null;
        List<FluxTable> query = client.getQueryApi().query(fluxQuery2);
        for (FluxTable table : query) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                Map<String, Object> values = record.getValues();

                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    if (entry.getKey().startsWith("_field") || entry.getKey().startsWith("_value")) {
                        Object fieldValue = entry.getValue();
                        if (entry.getKey().startsWith("_value")) {
                            temp_password = (String) fieldValue;
                        }
                        if (entry.getKey().startsWith("_field") && fieldValue.equals("password")) {
                            if (temp_password.equals(password)) {
                                System.out.println("Login successful for userId: " + userId);
                                return CommonResult.success(userId);
                            } else {
                                System.out.println("Login failed: Wrong Password!");
                                return CommonResult.failed("密码错误");
                            }
                        }
                    }
                }
            }
        }

        System.out.println("Login failed: Unknown Error");
        return CommonResult.failed("未知错误");
    }
}

