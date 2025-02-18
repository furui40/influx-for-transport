package com.example.demo.util;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class DBUtilSearch {
    // 查询数据
    public static void BaseQuery(InfluxDBClient client, String bucket,
                                 Long startTime, Long stopTime,
                                 String measurement, String field,
                                 String channel, String decoder,
                                 Boolean includeChannelCheck) {
        // 构建基本的 Flux 查询语句
        StringBuilder flux = new StringBuilder("from(bucket: \"" + bucket + "\") ");

        // 添加时间范围条件
        if (startTime != null && stopTime != null) {
            flux.append("|> range(start: ").append(startTime)
                    .append(", stop: ").append(stopTime).append(") ");
        }

        // 添加 measurement 过滤条件
        if (measurement != null && !measurement.isEmpty()) {
            flux.append("|> filter(fn: (r) => r._measurement == \"" + measurement + "\") ");
        }

        // 添加 field 过滤条件
        if (field != null && !field.isEmpty()) {
            flux.append("|> filter(fn: (r) => r._field == \"" + field + "\") ");
        }

        // 添加 channel 过滤条件
        if (channel != null && !channel.isEmpty()) {
            flux.append("|> filter(fn: (r) => r.channel == \"" + channel + "\") ");
        }

        // 添加 decoder 过滤条件
        if (decoder != null && !decoder.isEmpty()) {
            flux.append("|> filter(fn: (r) => r.decoder == \"" + decoder + "\") ");
        }

        // 添加是否检查 channel 字段的条件
        if (includeChannelCheck != null && includeChannelCheck) {
            flux.append("|> filter(fn: (r) => exists r.channel) ");
        }

        // 执行查询
        List<FluxTable> query = client.getQueryApi().query(flux.toString());

        // 输出查询结果
        for (FluxTable table : query) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                // 获取并打印每个 FluxRecord 的所有键值对
                Map<String, Object> values = record.getValues();

                // 遍历并打印 values 中的每个键值对
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    // 排除不需要的字段，如 "_start", "_stop", "_measurement", "_field", "table"
                    if (!entry.getKey().startsWith("_start")
                            && !entry.getKey().startsWith("_stop")
                            && !entry.getKey().startsWith("result")
                            && !entry.getKey().startsWith("_field")
                            && !entry.getKey().startsWith("_measurement")
                            && !entry.getKey().startsWith("table")) {
                        String fieldName = entry.getKey();
                        Object fieldValue = entry.getValue();
//                        System.out.println("Field Name: " + entry.getKey() + ", Value: " + fieldValue + ", Class: " + fieldValue.getClass().getName());
                        // 处理 '_time' 字段
                        if (fieldName.equals("_time") && fieldValue instanceof Instant) {
                            Instant utcTime = (Instant) fieldValue;  // 直接将 fieldValue 转为 Instant
                            ZonedDateTime localTime = utcTime.atZone(ZoneOffset.UTC).plusHours(8);  // 转为东八区时间
                            System.out.println("Field: " + fieldName + ", Value: " + localTime);
                        }
                        // 处理 'value' 字段，保留三位小数
                        else if (fieldName.equals("_value") && fieldValue instanceof Double) {
                            System.out.println("Field: " + fieldName + ", Value: " + String.format("%.3f", fieldValue));  // 使用 String.format 确保输出三位小数
                        }

                        // 其他字段直接输出
                        else {
                            System.out.println("Field: " + fieldName + ", Value: " + fieldValue);
                        }

                    }
                }
            }
        }
    }
}