package com.example.demo;

import com.example.demo.util.DBUtilSearch;
import com.example.demo.util.DBUtilUser;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.example.demo.util.DBUtilInsert.writeDataFromFile;


@Component
public class Test0 {

    // 注入 application.yml 中的配置
    @Value("${influxdb.url}")
    private String influxDbUrl;

    @Value("${influxdb.token}")
    private String influxDbToken;

    @Value("${influxdb.org}")
    private String influxDbOrg;

    @Value("${influxdb.bucket}")
    private String influxDbBucket;

    public void testInfluxDB() throws IOException {
        // 使用从配置文件中获取的值创建 InfluxDB 客户端
        InfluxDBClient client = InfluxDBClientFactory.create(influxDbUrl, influxDbToken.toCharArray(), influxDbOrg);

        // 文件路径
//        String filePath = "E:\\decoder\\01\\Wave_20240712_000000.txt";
//        try {
//            // 从文件中读取并写入数据
//            writeDataFromFile(client, filePath);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // 关闭客户端连接
//            client.close();
//        }

//        String bucket = "test2";
//        DBUtilSearch.BaseQuery(client,bucket, 1720713600L, 1720713601L,"sensor_data","value","Ch1","1",true);
//        client.close();

        // 注册新用户
//        String registrationResult = DBUtilUser.registerUser(client, "john_doe", "password123");
//        System.out.println(registrationResult);

        // 验证登录
        String loginResult = DBUtilUser.validateLogin(client, "john_doe", "password1234");
        System.out.println(loginResult);

    }
}
