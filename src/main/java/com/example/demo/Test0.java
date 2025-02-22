package com.example.demo;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.WeightData;
import com.example.demo.resolver.DynamicWeighing;
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

//        DBUtilInsert.testsearch(client,"1","01");
//        client.close();

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

//        String bucket = "test2";
//        DBUtilSearch.BaseQuery(client,bucket, 1720713600L, 1720713601L,"sensor_data","value","Ch1","1",true);
//        client.close();

        // 注册新用户
//        CommonResult registrationResult = DBUtilUser.registerUser(client, "123", "123");
//        System.out.println(registrationResult.getMessage());

        // 验证登录
//        CommonResult loginResult = DBUtilUser.validateLogin(client, "123", "123");
//        System.out.println(loginResult.getMessage() + loginResult.getData());


//        DataRevise.dataRevise(client, Instant.ofEpochSecond(1720749600L), Instant.ofEpochSecond(1720774800L));
//        System.out.println("finished");
//        client.close();

//        DataRevise2.dataRevise2(client, Instant.ofEpochSecond(1720749600L), Instant.ofEpochSecond(1720774800L));
//        System.out.println("finished");
//        client.close();

//        DynamicWeighing.processFile(client,"E:\\data\\2024_season1_weight\\2024_season1_weight\\20231104.xlsx");
//        System.out.println("finished");

        Instant startTime = Instant.parse("2023-11-04T00:17:27.000Z");
        Instant stopTime = Instant.parse("2023-11-04T14:28:37.000Z");
        List<WeightData> weightDataList = DynamicWeighing.queryWeightData(client,startTime,stopTime);
        // 打印查询结果
        for (WeightData weightData : weightDataList) {
            System.out.println(weightData);
        }
        client.close();
    }
}
