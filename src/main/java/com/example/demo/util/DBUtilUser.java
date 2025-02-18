package com.example.demo.util;

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
    public static String registerUser(InfluxDBClient client, String userName, String password) {
        // 创建查询语句，检查用户名是否已存在
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
            return "Registration failed: Username already exists.";
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
        return "User registered successfully with userId: " + newUser.getUserId();
    }

    // 登录验证方法
    public static String validateLogin(InfluxDBClient client, String userName, String password) {
        // 创建查询语句，检查用户名、密码是否匹配且用户状态为 active
        QueryApi queryApi = client.getQueryApi();
        String fluxQuery1 = "from(bucket: \"test2\") " +
                "|> range(start: 0) " +
                "|> filter(fn: (r) => r._measurement == \"user_data\") " +
                "|> filter(fn: (r) => r._field == \"user_name\" and r._value == \"" + userName + "\") ";

//        System.out.println(fluxQuery1);
        // 执行查询
        List<User> users = queryApi.query(fluxQuery1, User.class);

        // 判断是否找到符合条件的用户
        if (users.isEmpty()) {
            System.out.println("Invalid username");
            return "Login failed: Invalid username";
        }

        // 如果存在符合条件的用户，返回 user_id
        String userId = users.get(0).getUserId();

        String fluxQuery2 = "from(bucket: \"test2\") " +
                "|> range(start: 0) " +
                "|> filter(fn: (r) => r._measurement == \"user_data\") " +
                "|> filter(fn: (r) => r.user_id == \"" + userId + "\") ";

//        System.out.println(fluxQuery2);
        String temp_password = null;
        List<FluxTable> query = client.getQueryApi().query(fluxQuery2);
        for (FluxTable table : query) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                // 获取并打印每个 FluxRecord 的所有键值对
                Map<String, Object> values = record.getValues();

                // 遍历并打印 values 中的每个键值对
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    // 排除不需要的字段，如 "_start", "_stop", "_measurement", "_field", "table"
                    if (entry.getKey().startsWith("_field") || entry.getKey().startsWith("_value")) {
                        Object fieldValue = entry.getValue();
//                        System.out.println("Field Name: " + entry.getKey() + ", Value: " + fieldValue + ", Class: " + fieldValue.getClass().getName());
                        if (entry.getKey().startsWith("_value")) {
                            temp_password = (String) fieldValue;
//                            System.out.println("temp_password1 = " + temp_password);
                        }
                        if (entry.getKey().startsWith("_field") && fieldValue.equals("password")) {
                            System.out.println("temp_password2 = " + temp_password);
                            if (temp_password.equals(password)) {
                                System.out.println("Login successful for userId: " + userId);
                                return "Login successful, userId: " + userId;
                            }else{
                                System.out.println("Login failed:Wrong Password!");
                                return "Login failed:Wrong Password!";
                            }
                        }
                    }

                }
            }


        }
        System.out.println("Login failed:Unknown Error");
        return "Login failed:Unknown Error";
    }
}
