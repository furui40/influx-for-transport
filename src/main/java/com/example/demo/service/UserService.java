package com.example.demo.service;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.UserData;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.User;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Component
public class UserService {
    private static String influxDbOrg;
    private static String influxDbBucket;

    @Value("${influxdb.org}")
    public void setInfluxDbOrg(String org) {
        UserService.influxDbOrg = org;
    }

    @Value("${influxdb.bucket}")
    public void setInfluxDbBucket(String bucket) {
        UserService.influxDbBucket = bucket;
    }


    // 注册方法
    public static CommonResult<String> registerUser(InfluxDBClient client, String userName, String password) {
        // 创建查询语句，检查用户名是否已存在
        if(userName.length() == 0 || password.length() == 0){
            System.out.println("Username is null.");
            return CommonResult.failed("用户名或密码为空");
        }
        if (password.contains("hit1920")) {
            return CommonResult.failed("请更换密码");
        }
        QueryApi queryApi = client.getQueryApi();
        String fluxQuery = "from(bucket: \"" + influxDbBucket +"\") " +
                "|> range(start: 0) " +
                "|> filter(fn: (r) => r._measurement == \"user_data\") " +
                "|> filter(fn: (r) => r.user_name == \""+userName +"\") ";

        System.out.println(fluxQuery);
        // 执行查询，查看是否有重复用户名
        List<UserData> existingUsers = queryApi.query(fluxQuery, UserData.class);

        // 如果查询结果不为空，说明用户名已存在
        if (!existingUsers.isEmpty()) {
            System.out.println("Username already exists.");
            return CommonResult.failed("用户名已存在");
        }

        // 使用当前时间戳作为 userId
        String userId = String.valueOf(Instant.now().toEpochMilli());

        // 创建一个用户实例
        UserData newUser = new UserData()
                .setUserId(userId)
                .setUserName(userName)
                .setPassword(password)
                .setUserStatus("normal");  // 默认设置为普通用户

        // 创建并写入 Point 数据
        Point point = Point.measurement("user_data")
                .addTag("user_id", newUser.getUserId())
                .addTag("user_name", newUser.getUserName())
                .addField("password", newUser.getPassword())
                .addTag("user_status", newUser.getUserStatus())
                .time(Instant.now(), WritePrecision.NS);

        System.out.println(point.toLineProtocol());

        // 写入数据库
        client.getWriteApiBlocking().writePoint(influxDbBucket, influxDbOrg, point);

        // 返回注册成功的提示信息及userId
        return CommonResult.success(newUser.getUserId());
    }

    // 登录验证方法
    public static CommonResult<String> validateLogin(InfluxDBClient client, String userName, String password) {
        if (userName.isEmpty() || password.isEmpty()) {
            return CommonResult.failed("用户名或密码为空");
        }

        QueryApi queryApi = client.getQueryApi();
        String flux = "from(bucket: \"" + influxDbBucket + "\") " +
                "|> range(start: 0) " +
                "|> filter(fn: (r) => r._measurement == \"user_data\") " +
                "|> filter(fn: (r) => r.user_name == \"" + userName + "\")";

        List<UserData> users = queryApi.query(flux, UserData.class);

        if (users.isEmpty()) {
            return CommonResult.failed("用户名或密码错误");
        }

        UserData user = users.get(0);

        // 验证密码
        if (!password.equals(user.getPassword())) {
            return CommonResult.failed("用户名或密码错误");
        }

        // 如果是管理员账号，要求密码中包含特定口令，例如 "@adminKey"
        if ("admin".equalsIgnoreCase(user.getUserStatus())) {
            if (!password.contains("hit1920")) {
                return CommonResult.failed("管理员口令验证失败");
            }
        }

        // 返回 user_id 和 user_status（作为前端识别权限的依据）
        return CommonResult.success(user.getUserId() + "::" + user.getUserStatus());
    }


    public static CommonResult<String> modifyPassword(InfluxDBClient client, String userId, String password, String newpassword) {
        // 创建查询语句，检查用户名、密码是否匹配且用户状态为 active
        if(userId.length() == 0 || password.length() == 0 || newpassword.length() == 0){
            System.out.println("password is null.");
            return CommonResult.failed("新密码或旧密码为空");
        }
        QueryApi queryApi = client.getQueryApi();
        String fluxQuery1 = "from(bucket: \"" + influxDbBucket +"\") " +
                "|> range(start: 0) " +
                "|> filter(fn: (r) => r._measurement == \"user_data\") " +
                "|> filter(fn: (r) => r.user_id == \""+ userId +"\" ) ";

        System.out.println(fluxQuery1);
        List<UserData> users = queryApi.query(fluxQuery1, UserData.class);

        // 判断是否找到符合条件的用户
        if (users.isEmpty()) {
            System.out.println("Invalid username");
            return CommonResult.failed("用户名错误");
        }else{
            if(users.size() == 1){
                for(UserData user : users){
                    System.out.println(user);
                    if(user.getPassword().equals(password)){
                        Point point = Point.measurement("user_data")
                                .addTag("user_id", user.getUserId())
                                .addTag("user_name", user.getUserName())
                                .addField("password", newpassword)
                                .addTag("user_status", user.getUserStatus())
                                .time(user.getTimestamp(), WritePrecision.NS);
                        client.getWriteApiBlocking().writePoint(influxDbBucket, influxDbOrg, point);
                    }else return CommonResult.failed("密码错误");
                }
            }else return CommonResult.failed("未知错误1");
            return CommonResult.success(userId);
        }
    }
}

