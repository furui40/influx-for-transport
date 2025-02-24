package com.example.demo.entity;

import java.time.Instant;
import java.util.Map;

public class MonitorData {
    private Instant time; // 时间戳
    private Map<String, Double> fieldValues; // 字段值

    public MonitorData(Instant time, Map<String, Double> fieldValues) {
        this.time = time;
        this.fieldValues = fieldValues;
    }

    public Instant getTime() {
        return time;
    }

    public Map<String, Double> getFieldValues() {
        return fieldValues;
    }

    @Override
    public String toString() {
        return "MonitorData{" +
                "time=" + time +
                ", fieldValues=" + fieldValues +
                '}';
    }
}