package com.example.demo;

import com.example.demo.util.DBUtilSearch;
import com.example.demo.util.DBUtilUser;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
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

        // 解调器编号列表
//        int[] decoderNumbers = {1, 2, 3, 4};
//        String date = "20240712"; // 日期
//        String basePath = "E:\\decoder"; // 基础路径
//
//        try {
//            // 遍历所有解调器
//            for (int decoderNumber : decoderNumbers) {
//                String decoderId = String.format("%02d", decoderNumber); // 格式化为两位数，例如 "01"
//                String decoderPath = basePath + "\\" + decoderId; // 解调器路径
//
//                // 遍历 24 小时的数据文件
//                for (int hour = 0; hour < 24; hour++) {
//                    String hourStr = String.format("%02d0000", hour); // 格式化为小时，例如 "010000"
//                    String filePath = decoderPath + "\\Wave_" + date + "_" + hourStr + ".txt"; // 文件路径
//
//                    // 检查文件是否存在
//                    File file = new File(filePath);
//                    if (!file.exists()) {
//                        System.out.println("文件不存在: " + filePath);
//                        continue;
//                    }
//
//                    // 从文件中读取并写入数据
//                    System.out.println("正在处理文件: " + filePath);
//                    writeDataFromFile(client, filePath);
//                }
//            }
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
//        String loginResult = DBUtilUser.validateLogin(client, "john_doe", "password1234");
//        System.out.println(loginResult);

    }
}
