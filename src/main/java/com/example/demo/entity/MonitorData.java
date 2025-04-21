package com.example.demo.entity;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class MonitorData {
    private Instant time; // 时间戳
    private Map<String, Double> fieldValues; // 字段值
    private List<String> fields; // 字段顺序列表（可选）

    public MonitorData(Instant time, Map<String, Double> fieldValues) {
        this.time = time;
        this.fieldValues = fieldValues;
        this.fields = List.copyOf(fieldValues.keySet()); // 自动从fieldValues获取字段名
    }

    public MonitorData(Instant time, Map<String, Double> fieldValues, List<String> fields) {
        this.time = time;
        this.fieldValues = fieldValues;
        this.fields = fields;
    }

    public Instant getTime() {
        return time;
    }

    public Map<String, Double> getFieldValues() {
        return fieldValues;
    }

    public List<String> getFields() {
        return fields != null ? fields : List.copyOf(fieldValues.keySet());
    }

    public List<Double> getValues() {
        List<String> targetFields = getFields();
        return targetFields.stream()
                .map(field -> fieldValues.getOrDefault(field, Double.NaN))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "MonitorData{" +
                "time=" + time +
                ", fieldValues=" + fieldValues +
                '}';
    }
}