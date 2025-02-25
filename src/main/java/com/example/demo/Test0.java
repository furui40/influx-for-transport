package com.example.demo;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.MonitorData;
import com.example.demo.entity.WeatherData;
import com.example.demo.entity.WeightData;
import com.example.demo.resolver.DynamicWeighing;
import com.example.demo.resolver.Weather;
import com.example.demo.util.DBUtilInsert;
import com.example.demo.util.DBUtilSearch;
import com.example.demo.util.DBUtilUser;
import com.example.demo.util.DataRevise;
import com.example.demo.util.DataRevise2;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.example.demo.util.DBUtilInsert.writeDataFromFile2;
import static com.example.demo.util.DBUtilInsert.writeDataFromFile1;


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
        // 测试高频传感器查询
//        DBUtilInsert.testsearch(client,"1","01");
//        client.close();

        // 批量写入高频传感器
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
//                for (int hour = 10; hour < 16; hour++) {
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
//                    writeDataFromFile1(client, filePath);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // 关闭客户端连接
//            client.close();
//        }
        // 完整查询
//        String bucket = "test2";
//        DBUtilSearch.BaseQuery(client,bucket, 1720713600L, 1720713601L,"sensor_data","value","Ch1","1",true);
//        client.close();

        // 注册新用户
//        CommonResult registrationResult = DBUtilUser.registerUser(client, "123", "123");
//        System.out.println(registrationResult.getMessage());

        // 验证登录
//        CommonResult loginResult = DBUtilUser.validateLogin(client, "123", "123");
//        System.out.println(loginResult.getMessage() + loginResult.getData());

        // 跨解调器的土压力传感器数据修正
//        DataRevise.dataRevise(client, Instant.ofEpochSecond(1720749600L), Instant.ofEpochSecond(1720774800L));
//        System.out.println("finished");

//        client.close();
        // 多个初始值的主传感器数据修正
//        DataRevise2.dataRevise2(client, Instant.ofEpochSecond(1720749600L), Instant.ofEpochSecond(1720774800L));
//        System.out.println("finished");
//        client.close();

        // 动态称重数据写入
//        DynamicWeighing.processFile(client,"E:\\data\\2024_season1_weight\\2024_season1_weight\\20240712.xlsx");
//        System.out.println("finished");

        // 动态称重数据查询
//        long startTime = Instant.parse("2024-07-11T20:00:00.000Z").toEpochMilli()/1000;
//        long stopTime = Instant.parse("2024-07-12T10:00:00.000Z").toEpochMilli()/1000;
//        List<WeightData> weightDataList = DynamicWeighing.queryWeightData(client,startTime,stopTime);
//        // 打印查询结果
//        for (WeightData weightData : weightDataList) {
//            System.out.println(weightData);
//        }

        // 新高频传感器数据查询
//        List<String> fields = Arrays.asList("1_Ch1_ori", "2_Ch2_act");
//        Long startTime = 1720749600L; // 2024-07-12T02:00:00Z
//        Long stopTime = 1720749601L;  // 2024-07-12T02:00:01Z
//        List<MonitorData> result = DBUtilSearch.BaseQuery(client, fields, startTime, stopTime);
//        // 输出结果
//        for (MonitorData data : result) {
//            System.out.println(data);
//        }

        // 气象数据写入
//        Weather.processFile(client,"E:\\data\\Weather\\202407.xlsx");
//        System.out.println("finished");

        // 气象数据查询
//        long startTime = Instant.parse("2024-07-12T00:00:00.000Z").toEpochMilli()/1000;
//        long stopTime = Instant.parse("2024-07-13T00:00:00.000Z").toEpochMilli()/1000;
//        List<WeatherData> weatherDataList = Weather.queryWeatherData(client,startTime,stopTime);
//        // 打印查询结果
//        for (WeatherData weatherData : weatherDataList) {
//            System.out.println(weatherData);
//        }

        client.close();
    }
}
