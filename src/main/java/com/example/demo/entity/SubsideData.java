package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
@Measurement(name = "subside_data")
public class SubsideData {
    @Column(name = "_time", timestamp = true)
    private Instant timestamp;

    // 存储多个测点的沉降数据
    private Map<String, Double> fieldValues = new HashMap<>();
}