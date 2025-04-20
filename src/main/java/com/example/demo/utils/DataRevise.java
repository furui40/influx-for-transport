package com.example.demo.utils;

import com.example.demo.entity.SensorParams;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static com.example.demo.common.ItemMapping.sensorParamsMap;

public class DataRevise {

    private static final int PAGE_SIZE_SECONDS = 100;
    private static final int BATCH_SIZE = 10000;
    private static final String influxDbBucket = "test7";
    private static final String influxDbOrg = "test";

    private static final Map<String, CrossDecoderConfig> CONFIG_MAP = new HashMap<>() {{
        put("1_Ch27", new CrossDecoderConfig("2", "Ch20"));
        put("1_Ch28", new CrossDecoderConfig("2", "Ch20"));
        put("1_Ch29", new CrossDecoderConfig("2", "Ch20"));
        put("3_Ch27", new CrossDecoderConfig("4", "Ch20"));
        put("3_Ch28", new CrossDecoderConfig("4", "Ch20"));
        put("3_Ch29", new CrossDecoderConfig("4", "Ch20"));
    }};

    public static void dataRevise(InfluxDBClient client, Instant startTime, Instant stopTime, int method) throws IOException {
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        QueryApi queryApi = client.getQueryApi();

        Instant currentStart = startTime;
        while (currentStart.isBefore(stopTime)) {
            Instant currentEnd = currentStart.plusSeconds(PAGE_SIZE_SECONDS);
            if (currentEnd.isAfter(stopTime)) {
                currentEnd = stopTime;
            }

            // 根据method参数构建不同的Flux查询条件
            StringBuilder fluxFilter = new StringBuilder();
            switch (method) {
                case 1: // 只查询第一组(decoder1和decoder2)
                    fluxFilter.append("(r.decoder == \"1\" and (r._field == \"Ch27_ori\" or r._field == \"Ch28_ori\" or r._field == \"Ch29_ori\"))\n")
                            .append("    or (r.decoder == \"2\" and r._field == \"Ch20_ori\")");
                    break;
                case 2: // 只查询第二组(decoder3和decoder4)
                    fluxFilter.append("(r.decoder == \"3\" and (r._field == \"Ch27_ori\" or r._field == \"Ch28_ori\" or r._field == \"Ch29_ori\"))\n")
                            .append("    or (r.decoder == \"4\" and r._field == \"Ch20_ori\")");
                    break;
                case 3: // 查询两组(默认行为)
                default:
                    fluxFilter.append("(r.decoder == \"1\" and (r._field == \"Ch27_ori\" or r._field == \"Ch28_ori\" or r._field == \"Ch29_ori\"))\n")
                            .append("    or (r.decoder == \"2\" and r._field == \"Ch20_ori\")\n")
                            .append("    or (r.decoder == \"3\" and (r._field == \"Ch27_ori\" or r._field == \"Ch28_ori\" or r._field == \"Ch29_ori\"))\n")
                            .append("    or (r.decoder == \"4\" and r._field == \"Ch20_ori\")");
                    break;
            }

            String flux = String.format(
                    "from(bucket: \"%s\")\n" +
                            "|> range(start: %s, stop: %s)\n" +
                            "|> filter(fn: (r) => (\n" +
                            "    %s))\n" +
                            "|> pivot(rowKey: [\"_time\"], columnKey: [\"decoder\", \"_field\"], valueColumn: \"_value\")",
                    influxDbBucket,
                    currentStart.getEpochSecond(),
                    currentEnd.getEpochSecond(),
                    fluxFilter.toString()
            );

            List<FluxTable> tables = queryApi.query(flux);
            List<Point> batchPoints = new ArrayList<>(BATCH_SIZE);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Instant time = (Instant) record.getValueByKey("_time");

                    Map<String, Double> valueMap = new HashMap<>();
                    for (String key : record.getValues().keySet()) {
                        if (key.startsWith("1_") || key.startsWith("2_") ||
                                key.startsWith("3_") || key.startsWith("4_")) {
                            Object val = record.getValueByKey(key);
                            if (val instanceof Number) {
                                valueMap.put(key, ((Number) val).doubleValue());
                            }
                        }
                    }

                    // 根据method参数决定处理哪些通道
                    for (String key : CONFIG_MAP.keySet()) {
                        String decoder = key.split("_")[0];
                        if ((method == 1 && (decoder.equals("1") || decoder.equals("2"))) ||
                                (method == 2 && (decoder.equals("3") || decoder.equals("4"))) ||
                                (method == 3)) {
                            processChannel(key, time, valueMap, batchPoints);
                        }
                    }

                    if (batchPoints.size() >= BATCH_SIZE) {
//                        writeBatchToFile(batchPoints);
                        writeApi.writePoints(influxDbBucket, influxDbOrg, batchPoints);
                        batchPoints.clear();
                    }
                }
            }

            if (!batchPoints.isEmpty()) {
//                writeBatchToFile(batchPoints);
                writeApi.writePoints(influxDbBucket, influxDbOrg, batchPoints);
            }

            currentStart = currentEnd;
        }
    }

    private static void processChannel(String channelKey,
                                       Instant time, Map<String, Double> valueMap,
                                       List<Point> batchPoints) {
        String[] parts = channelKey.split("_");
        String decoder = parts[0];
        String channel = parts[1];
        String targetField = channel + "_rev1";

        SensorParams params = sensorParamsMap.get('0'+channelKey);
        if (params == null) return;

        CrossDecoderConfig config = CONFIG_MAP.get(channelKey);
        if (config == null) return;

        String currentKey = decoder + "_" + channel + "_ori";
        Double lambda = valueMap.get(currentKey);
        if (lambda == null) return;

        String sourceKey = config.sourceDecoder + "_" + config.sourceChannel + "_ori";
        Double lambdaT = valueMap.get(sourceKey);
        if (lambdaT == null) return;

        String sourceParamKey = config.sourceDecoder + "_" + config.sourceChannel;
        SensorParams paramsT = sensorParamsMap.get('0'+sourceParamKey);
        if (paramsT == null) return;

        double revisedValue = calculateP(
                lambda,
                params.getLambda0(),
                lambdaT,
                paramsT.getLambda0(),
                params.getA(),
                params.getB(),
                params.getK()
        );

        Point point = Point.measurement("sensor_data")
                .addTag("decoder", decoder)
                .addField(targetField, revisedValue)
                .time(time, WritePrecision.NS);

        batchPoints.add(point);
    }

    private static double calculateP(double lambda, double lambda0,
                                     double lambdaT, double lambdaTPrime,
                                     double a, double b, double K) {
        return (lambda - lambda0 - (lambdaT - lambdaTPrime) * b / a) / K;
    }

    private static class CrossDecoderConfig {
        String sourceDecoder;
        String sourceChannel;

        public CrossDecoderConfig(String sourceDecoder, String sourceChannel) {
            this.sourceDecoder = sourceDecoder;
            this.sourceChannel = sourceChannel;
        }
    }

    private static void writeBatchToFile(List<Point> batchLines) throws IOException {
        // 获取文件路径中的目录部分
        String outputFilePath = "E:\\decoder\\01\\2.txt";

        // 打开文件并写入数据
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            for (Point line : batchLines) {
                writer.write(line.toLineProtocol());
                writer.newLine();
            }
        }
    }
}
