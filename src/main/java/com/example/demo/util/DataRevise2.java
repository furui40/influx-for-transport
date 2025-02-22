package com.example.demo.util;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataRevise2 {

    private static final int PAGE_SIZE_SECONDS = 10; // 分页大小（秒）
    private static final int BATCH_SIZE = 10000; // 批量写入大小
    private static final String influxDbBucket = "test2";
    private static final String influxDbOrg = "test";

    // 固定 K 值
    private static final double K = 0.0012;

    // 定义每个信道的 λi 参数
    private static final Map<String, List<Double>> CHANNEL_LAMBDA0 = new HashMap<>() {{
        // 解调器
        put("1_Ch23", List.of(1529.645,1534.488,1538.975,1542.847,1549.705,1554.711)); // 示例参数
        put("1_Ch25", List.of(1529.528,1534.065)); // 示例参数
        put("1_Ch26", List.of(1539.712,1543.036,1549.300,1554.618)); // 示例参数
        // 解调器2
        put("2_Ch22", List.of(1529.148,1533.814,1537.012,1542.722,1549.245,1553.962)); // 示例参数
        put("2_Ch24", List.of(1529.855,1534.574,1538.843,1542.549,1549.001,1553.285)); // 示例参数
        // 解调器3
        put("3_Ch23", List.of(1529.411,1533.599,1537.111,1542.503,1549.380,1553.975)); // 示例参数
        put("3_Ch25", List.of(1529.639,1534.548)); // 示例参数
        put("3_Ch26", List.of(1539.086,1544.705,1549.113,1554.048)); // 示例参数
        // 解调器4
        put("4_Ch22", List.of(1529.473,1533.463,1537.495,1543.118,1549.187,1554.132)); // 示例参数
        put("4_Ch24", List.of(1529.197,1534.510)); // 示例参数
        put("4_Ch25", List.of(1539.577,1543.151,1549.466,1554.588)); // 示例参数
    }};

    public static void dataRevise2(InfluxDBClient client, Instant startTime, Instant stopTime) {
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        QueryApi queryApi = client.getQueryApi();
        long start = System.currentTimeMillis();
        Instant currentStart = startTime;
        while (currentStart.isBefore(stopTime)) {
            Instant currentEnd = currentStart.plusSeconds(PAGE_SIZE_SECONDS);
            if (currentEnd.isAfter(stopTime)) {
                currentEnd = stopTime;
            }

            // 1. 查询原始数据
            String flux = String.format(
                    "from(bucket: \"%s\")\n" +
                            "|> range(start: %s, stop: %s)\n" +
                            "|> filter(fn: (r) => (\n" +
                            "    (r.decoder == \"1\" and (r._field == \"Ch23_ori\" or r._field == \"Ch25_ori\" or r._field == \"Ch26_ori\"))\n" +
                            "    or (r.decoder == \"2\" and (r._field == \"Ch22_ori\" or r._field == \"Ch24_ori\"))\n" +
                            "    or (r.decoder == \"3\" and (r._field == \"Ch23_ori\" or r._field == \"Ch25_ori\" or r._field == \"Ch26_ori\"))\n" +
                            "    or (r.decoder == \"4\" and (r._field == \"Ch22_ori\" or r._field == \"Ch24_ori\" or r._field == \"Ch25_ori\"))))\n" +
                            "|> pivot(rowKey: [\"_time\"], columnKey: [\"decoder\", \"_field\"], valueColumn: \"_value\")",
                    influxDbBucket,
                    currentStart.getEpochSecond(),
                    currentEnd.getEpochSecond()
            );
            System.out.println("start:" + currentStart.getEpochSecond() + " stop:" + currentEnd.getEpochSecond());
            List<FluxTable> tables = queryApi.query(flux);
            System.out.println("查询结束累计用时：" + (System.currentTimeMillis() - start)/1000.0 + "s");
            // 2. 处理并计算修正值
            List<Point> batchPoints = new ArrayList<>(BATCH_SIZE);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Instant time = (Instant) record.getValueByKey("_time");

                    // 构建交叉数据映射
                    Map<String, Double> valueMap = new HashMap<>();
                    for (String key : record.getValues().keySet()) {
                        if (key.startsWith("1_") || key.startsWith("2_") ||
                                key.startsWith("3_") || key.startsWith("4_")) {
                            valueMap.put(key, (Double) record.getValueByKey(key));
                        }
                    }

                    // 处理需要修正的信道
                    processChannel(writeApi, "1_Ch23", time, valueMap, batchPoints);
                    processChannel(writeApi, "1_Ch25", time, valueMap, batchPoints);
                    processChannel(writeApi, "1_Ch26", time, valueMap, batchPoints);
                    processChannel(writeApi, "2_Ch22", time, valueMap, batchPoints);
                    processChannel(writeApi, "2_Ch24", time, valueMap, batchPoints);
                    processChannel(writeApi, "3_Ch23", time, valueMap, batchPoints);
                    processChannel(writeApi, "3_Ch25", time, valueMap, batchPoints);
                    processChannel(writeApi, "3_Ch26", time, valueMap, batchPoints);
                    processChannel(writeApi, "4_Ch22", time, valueMap, batchPoints);
                    processChannel(writeApi, "4_Ch24", time, valueMap, batchPoints);
                    processChannel(writeApi, "4_Ch25", time, valueMap, batchPoints);

                    // 批量写入
                    if (batchPoints.size() >= BATCH_SIZE) {
                        writeApi.writePoints(influxDbBucket, influxDbOrg, batchPoints);
                        batchPoints.clear();
                    }
                }
            }

            // 写入剩余数据
            if (!batchPoints.isEmpty()) {
                writeApi.writePoints(influxDbBucket, influxDbOrg, batchPoints);
                System.out.println("写入结束累计用时：" + (System.currentTimeMillis() - start)/1000.0 + "s");
            }

            currentStart = currentEnd;
        }
    }

    private static void processChannel(WriteApiBlocking writeApi, String channelKey,
                                       Instant time, Map<String, Double> valueMap,
                                       List<Point> batchPoints) {
        String[] parts = channelKey.split("_");
        String decoder = parts[0];
        String channel = parts[1];

        // 获取当前信道的原始值
        Double lambda = valueMap.get(decoder + "_" + channel + "_ori");
        if (lambda == null) return;

        // 获取当前信道的 λi 参数
        List<Double> lambda0List = CHANNEL_LAMBDA0.get(channelKey);
        if (lambda0List == null || lambda0List.isEmpty()) return;

        // 计算修正值并写入数据库
        for (int i = 0; i < lambda0List.size(); i++) {
            double lambda0 = lambda0List.get(i);
            double revisedValue = calculateT(lambda, lambda0, K);

            // 构建数据点
            Point point = Point.measurement("sensor_data")
                    .addTag("decoder", decoder.toString()) // 去掉前缀 "0"
                    .addField(channel + "_rev" + (i + 1), revisedValue) // 修正值字段
                    .time(time, WritePrecision.NS);

            batchPoints.add(point);
        }
    }

    private static double calculateT(double lambda, double lambda0, double K) {
        return (lambda - lambda0) / K;
    }
}