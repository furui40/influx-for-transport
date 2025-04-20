package com.example.demo.utils;

import com.example.demo.entity.MonitorData;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBUtilSearch {

    /**
     * 查询数据
     *
     * @param client    InfluxDB 客户端
     * @param fields    需要查询的字段列表（如 ["1_Ch1_ori", "2_Ch2_act"]）
     * @param startTime 查询起始时间（时间戳，秒）
     * @param stopTime  查询结束时间（时间戳，秒）
     * @return 查询结果列表
     */
    public static List<MonitorData> BaseQuery(InfluxDBClient client, List<String> fields,
                                              Long startTime, Long stopTime, Long samplingInterval) {
        // 构建 Flux 查询语句
        StringBuilder flux = new StringBuilder("from(bucket: \"test7\") ")
                .append("|> range(start: ").append(startTime)
                .append(", stop: ").append(stopTime).append(") ")
                .append("|> filter(fn: (r) => ");

        // 添加字段过滤条件
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            String[] parts = field.split("_");
            String decoder = parts[0];
            String channel = parts[1];
            String fieldType = parts[2];

            flux.append("(r.decoder == \"").append(decoder)
                    .append("\" and r._field == \"")
                    .append(channel).append("_").append(fieldType).append("\")");

            if (i < fields.size() - 1) {
                flux.append(" or ");
            }
        }
        flux.append(") ").append(" |> sample(n: ").append(samplingInterval).append(", pos: 0 )");
        flux.append("|> pivot(rowKey: [\"_time\"], columnKey: [\"decoder\", \"_field\"], valueColumn: \"_value\")");

        System.out.println(flux);

        // 执行查询
        List<FluxTable> tables = client.getQueryApi().query(flux.toString());

        // 解析查询结果
        List<MonitorData> result = new ArrayList<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Instant time = (Instant) record.getValueByKey("_time");
                Map<String, Double> fieldValues = new HashMap<>();

                // 提取字段值
                for (String field : fields) {
                    Double value = (Double) record.getValueByKey(field);
                    if (value != null) {
                        fieldValues.put(field, value);
                    }
                }

                // 构建 MonitorData 对象
                MonitorData data = new MonitorData(time, fieldValues);
                result.add(data);
            }
        }

        return result;
    }

}