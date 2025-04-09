package com.example.demo.utils;

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

public class DataRevise {

    private static final int PAGE_SIZE_SECONDS = 100; // 分页大小（秒）
    private static final int BATCH_SIZE = 10000; // 批量写入大小
    private static final String influxDbBucket = "test6";

    private static final String influxDbOrg = "test";

    // 需要修正的信道配置
    private static final Map<String, CrossDecoderConfig> CONFIG_MAP = new HashMap<>() {{
        // 解调器1的信道配置
        put("1_Ch27", new CrossDecoderConfig("2_Ch20", "2_Ch20"));
        put("1_Ch28", new CrossDecoderConfig("2_Ch20", "2_Ch20"));
        put("1_Ch29", new CrossDecoderConfig("2_Ch20", "2_Ch20"));
        // 解调器3的信道配置
        put("3_Ch27", new CrossDecoderConfig("4_Ch20", "4_Ch20"));
        put("3_Ch28", new CrossDecoderConfig("4_Ch20", "4_Ch20"));
        put("3_Ch29", new CrossDecoderConfig("4_Ch20", "4_Ch20"));
    }};

    public static void dataRevise(InfluxDBClient client, Instant startTime, Instant stopTime) {
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        QueryApi queryApi = client.getQueryApi();

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
                            "    (r.decoder == \"1\" and (r._field == \"Ch27_ori\" or r._field == \"Ch28_ori\" or r._field == \"Ch29_ori\"))\n" +
                            "    or (r.decoder == \"2\" and r._field == \"Ch20_ori\")\n" +
                            "    or (r.decoder == \"3\" and (r._field == \"Ch27_ori\" or r._field == \"Ch28_ori\" or r._field == \"Ch29_ori\"))\n" +
                            "    or (r.decoder == \"4\" and r._field == \"Ch20_ori\")))\n" +
                            "|> pivot(rowKey: [\"_time\"], columnKey: [\"decoder\", \"_field\"], valueColumn: \"_value\")",
                    influxDbBucket,
                    currentStart.getEpochSecond(),
                    currentEnd.getEpochSecond()
            );
            System.out.println("start:" + currentStart.getEpochSecond() + "stop:" + currentEnd.getEpochSecond());
            List<FluxTable> tables = queryApi.query(flux);

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
                    processChannel(writeApi, "1_Ch27", time, valueMap, batchPoints);
                    processChannel(writeApi, "1_Ch28", time, valueMap, batchPoints);
                    processChannel(writeApi, "1_Ch29", time, valueMap, batchPoints);
                    processChannel(writeApi, "3_Ch27", time, valueMap, batchPoints);
                    processChannel(writeApi, "3_Ch28", time, valueMap, batchPoints);
                    processChannel(writeApi, "3_Ch29", time, valueMap, batchPoints);

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
        String targetField = channel + "_rev1";

        // 获取当前信道参数
        SensorParams params = SensorParamsLoader.getParams(channelKey);
        if (params == null) return;

        // 获取关联配置
        CrossDecoderConfig config = CONFIG_MAP.get(channelKey);
        if (config == null) return;

        // 获取原始值
        Double lambda = valueMap.get(decoder + "_" + channel + "_ori");
        if (lambda == null) return;

        // 获取关联信道值
        String s1 = config.sourceDecoder()+ "_ori";
        Double lambdaT = valueMap.get(s1);
        SensorParams paramsT = SensorParamsLoader.getParams(config.sourceDecoder());
        if (lambdaT == null || paramsT == null) return;

        // 执行计算
        double revisedValue = calculateP(
                lambda,
                params.lambda0(),
                lambdaT,
                paramsT.lambda0(),
                params.a(),
                params.b(),
                params.K()
        );

        // 构建数据点
        Point point = Point.measurement("sensor_data")
                .addTag("decoder", decoder.toString())
                .addField(targetField, revisedValue)
                .time(time, WritePrecision.NS);

        batchPoints.add(point);
    }

    private static double calculateP(double lambda, double lambda0,
                                     double lambdaT, double lambdaTPrime,
                                     double a, double b, double K) {
        return (lambda - lambda0 - (lambdaT - lambdaTPrime) * b / a) / K;
    }

    // 交叉解调器配置记录
    private record CrossDecoderConfig(String sourceDecoder, String sourceChannel) {
        // 添加方法：返回参数键
        public String paramsKey() {
            return sourceDecoder + "_" + sourceChannel;
        }
    }

    // 参数加载类
    private static class SensorParamsLoader {
        private static final Map<String, SensorParams> PARAMS_MAP = new HashMap<>() {{
            put("1_Ch27", new SensorParams(1537.130, 0.662987, 0.00964, 0.02547));
            put("1_Ch28", new SensorParams(1539.818, 1.502559, 0.00964, 0.02737));
            put("1_Ch29", new SensorParams(1543.889, 1.035090, 0.00964, 0.02681));
            put("3_Ch27", new SensorParams(1538.587, 0.917356, 0.00974, 0.02099));
            put("3_Ch28", new SensorParams(1543.069, 1.534425, 0.00974, 0.02524));
            put("3_Ch29", new SensorParams(1551.255, 1.565445, 0.02874, 0.00974));
            put("2_Ch20", new SensorParams(1553.875, 1.0, 1.0, 1.0)); // 补充示例参数
            put("4_Ch20", new SensorParams(1559.858, 1.0, 1.0, 1.0)); // 补充示例参数
        }};

        public static SensorParams getParams(String key) {
            return PARAMS_MAP.get(key);
        }
    }

    // 传感器参数记录类
    public record SensorParams(
            double lambda0,
            double K,
            double a,
            double b
    ) {}
}