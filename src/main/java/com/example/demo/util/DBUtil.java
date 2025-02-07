package com.example.demo.util;

import com.example.demo.entity.MonitorData;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class DBUtil {

    // 写入测试数据
    public static void writeData(InfluxDBClient client) {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        List<MonitorData> list = new ArrayList<>();

        // 初始化时间和经纬度
        Instant currentTime = Instant.now(); // 第一个数据点的时间为当前时间
        BigDecimal initialLongitude = new BigDecimal("113.12313");
        BigDecimal initialLatitude = new BigDecimal("23.8524");

        // 随机数生成器
        Random random = new Random();

        // 生成100条模拟数据
        for (int i = 0; i < 5; i++) {
            // 每个数据点时间递增
            currentTime = currentTime.plusMillis(random.nextInt(1001));  // 随机时间间隔0到1000毫秒

            // 每个数据点经纬度递增
            BigDecimal longitude = initialLongitude.add(BigDecimal.valueOf(i * 0.0001));  // 经度递增
            BigDecimal latitude = initialLatitude.add(BigDecimal.valueOf(i * 0.0001));  // 纬度递增

            // 创建数据对象并设置值
            MonitorData position = new MonitorData()
                    .setDeviceId("123")
                    .setVehicleId("321")
                    .setLocationTime(currentTime)
                    .setLongitude(longitude)
                    .setLatitude(latitude);

            // 添加到数据列表
            list.add(position);
        }

        // 写入数据到 InfluxDB (measurement: "test", bucket: "position1")
        writeApiBlocking.writeMeasurements("position1", "test", WritePrecision.NS, list);
        log.info("Data written successfully.");
    }

    // 查询数据
    public static void queryData(InfluxDBClient client) {
        String flux = "from(bucket: \"position1\") "  // 这里双引号需要转义
                + "  |> range(start: 0) " ; // 查询时间范围
//                + "  |> filter(fn: (r) => r[\"_measurement\"] == \"test\")"  // 过滤 _measurement
//                + "  |> pivot(rowKey:[\"_time\"],columnKey: [\"_field\"],valueColumn: \"_value\")";
        // 执行查询
        List<FluxTable> query = client.getQueryApi().query(flux);

        // 输出查询结果
        for (FluxTable table : query) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
//                log.info("Measurement: {}, Field: {}, Value: {}, Time: {}",
//                        record.getMeasurement(),
//                        record.getField(),
//                        record.getValue(),
//                        record.getTime());
                System.out.println("Measurement: " + record.getMeasurement() +
                        ", Field: " + record.getField() +
                        ", Value: " + record.getValue() +
                        ", Time: " + record.getTime());

            }
        }
    }

    // 测试写入和查询功能
    public static void test(InfluxDBClient client) {
        // 1. 写入数据
        writeData(client);

        // 2. 查询数据
        queryData(client);
    }
}
