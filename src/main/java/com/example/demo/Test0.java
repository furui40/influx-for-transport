package com.example.demo;

import com.example.demo.service.HighSensorService;
import com.example.demo.utils.*;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ExecutionException;


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

    public void testInfluxDB() throws Exception {
        // 使用从配置文件中获取的值创建 InfluxDB 客户端
        InfluxDBClient client = InfluxDBClientFactory.create(influxDbUrl, influxDbToken.toCharArray(), influxDbOrg);
        Long time1 = System.currentTimeMillis();
        System.out.println("started!");

        // 高频传感器写入
//        HighSensorService.processFile(client,"E:\\decoder\\01\\Wave_20240712_100000.txt");
//        MultiInsert.writeDataFromFile3(client,"E:\\decoder\\01\\Wave_20240712_000000.txt");
        DBUtilInsert.writeDataFromFile1(client,"E:\\decoder\\01\\Wave_20240712_010000.txt");
//        DBUtilInsert.writeDataFromFile2(client,"E:\\decoder\\01\\Wave_20240712_010000.txt");
//        DBUtilInsert.processAndWriteFile("E:\\decoder\\01\\Wave_20240712_000000_trans.txt",client);
//        DBUtilInsert.processAndWriteFile1(client,"E:\\decoder\\01\\Wave_20240712_000000.txt",320000);
//        DBUtilInsert.writeDataFromFile0(client,"E:\\decoder\\01\\Wave_20240712_000000.txt",3_600_000);
//        DBUtilInsert.writeDataFromFile0(client,"E:\\decoder\\01\\Wave_20240712_010000.txt",3_300_000);
//        DBUtilInsert.writeDataFromFile0(client,"E:\\decoder\\01\\Wave_20240712_020000.txt",3_400_000);
//        SimpleMultiInsert.writeDataFromFile(client,"E:\\decoder\\01\\Wave_20240712_000000.txt");
//        SimpleMultiInsert.writeDataFromFile1(client,"E:\\decoder\\01\\Wave_20240712_000000.txt");
//        Test2.writeDataFromFile3(client,"E:\\decoder\\01\\Wave_20240712_110000.txt");
//        client.close();

        // 测试高频传感器查询
//        DBUtilInsert.testsearch(client,"1","01");
//        client.close();

         //批量写入高频传感器
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
//                    writeDataFromFile1(client, filePath);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // 关闭客户端连接
//            client.close();
//        }

        //多线程写入
//        testUtil.writeDataFromFile(client,"E:\\decoder\\01\\Wave_20240712_000000.txt");
//        client.close();

        // 完整查询
//        String bucket = "test2";
//        DBUtilSearch.BaseQuery(client, Collections.singletonList("Ch1"),1720713600L, 1720713601L,1L);
//        client.close();

        // 注册新用户
//        CommonResult registrationResult = DBUtilUser.registerUser(client, "123", "123");
//        System.out.println(registrationResult.getMessage());

        // 验证登录
//        CommonResult loginResult = DBUtilUser.validateLogin(client, "123", "123");
//        System.out.println(loginResult.getMessage() + loginResult.getData());

        // 跨解调器的土压力传感器数据修正
//        DataRevise.dataRevise(client, Instant.ofEpochSecond(1720749600L), Instant.ofEpochSecond(1720749700L),3);
//        System.out.println("finished");

//        client.close();
        // 多个初始值的主传感器数据修正
//        DataRevise2.dataRevise2(client, Instant.ofEpochSecond(1720749600L), Instant.ofEpochSecond(1720753200L));
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
//        WeatherService.processFile(client,"E:\\data\\2024二三季度数据\\气象\\4\\4月.xlsx");

        // 气象数据查询
//        long startTime = Instant.parse("2024-07-12T00:00:00.000Z").toEpochMilli()/1000;
//        long stopTime = Instant.parse("2024-07-13T00:00:00.000Z").toEpochMilli()/1000;
//        List<WeatherData> weatherDataList = Weather.queryWeatherData(client,startTime,stopTime);
//        // 打印查询结果
//        for (WeatherData weatherData : weatherDataList) {
//            System.out.println(weatherData);
//        }

        // 下载申请审核
//        DownloadApply apply = new DownloadApply()
//                .setApplyId("1740568599042")
//                .setDataType("高频传感器数据")
//                .setFields("1111")
//                .setStartTime("111111111111")
//                .setStopTime("111111111112")
//                .setUserEmail("userEmail")
//                .setUserId("22222")
//                .setStatus("已审核")
//                .setMsg("已申请");
//
//        Point point = Point.measurement("apply_data")
//                .addTag("apply_id", apply.getApplyId())
//                .addField("status", apply.getStatus())
//                .addTag("user_id", apply.getUserId())
//                .addField("data_type", apply.getDataType())
//                .addField("fields", apply.getFields())
//                .addField("start_time", apply.getStartTime().toString())
//                .addField("stop_time", apply.getStopTime().toString())
//                .addField("user_email", apply.getUserEmail())
//                .addField("msg",apply.getMsg())
//                .time(Instant.parse("2024-07-12T00:00:00.000Z"), WritePrecision.NS); // 使用 applyId 作为时间戳
//
//        WriteApi writeApi = client.getWriteApi();
//
//        writeApi.writePoint(influxDbBucket, influxDbOrg, point);
//        DownloadService.passApply(client,"1740568599042");

        // 修改密码功能
//        UserService.modifyPassword(client, "1740650168866", "123","1234");

        // 沉降,孔隙水压力
//        JinMaDataService.processFile(client,"E:\\data\\2024二三季度数据\\金玛\\沉降\\7.xlsx","subside");
//        JinMaDataService.processFile(client,"E:\\data\\2024二三季度数据\\金玛\\孔隙水压力\\7.xlsx","waterPressure");
//        JinMaDataService.processFile(client,"E:\\data\\2024二三季度数据\\金玛\\温湿度\\7.xlsx","humiture");
//        long startTime = Instant.parse("2024-07-12T00:00:00.000Z").toEpochMilli()/1000;
//        long stopTime = Instant.parse("2024-07-13T00:00:00.000Z").toEpochMilli()/1000;
//        List<String> fields = Arrays.asList("0034230033-01","0034230033-02") ; //沉降
//        List<String> fields = Arrays.asList("0034230583-01","0034230583-02") ; //孔隙水压力
//        List<String> fields = Arrays.asList("0034230034-01","0034230034-02") ; //温湿度
//        List<JinMaData> jinMaDataList = JinMaDataService.queryJinMaData(client,startTime,stopTime,fields, "subside");//沉降
//        List<JinMaData> jinMaDataList = JinMaDataService.queryJinMaData(client,startTime,stopTime,fields, "waterPressure");//孔隙水压力
//        List<JinMaData> jinMaDataList = JinMaDataService.queryJinMaData(client,startTime,stopTime,fields, "humiture");//温湿度
//        for(JinMaData jinMaData:jinMaDataList){
//            System.out.println(jinMaData);
//        }


        Long time2 = System.currentTimeMillis();
        System.out.println("finished!");
        System.out.println("用时：" + (time2-time1)/1000.0 + "秒");
        client.close();
    }
}
