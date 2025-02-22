package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true)
@Measurement(name = "sensor_data")
public class MonitorData {

    /**
     * 解调器id
     */
    @Column(name = "decoder_id", tag = true)
    private String decoderId;

    /**
     * 信道id
     */
    @Column(name = "channel_id", tag = true)
    private String channelId;

    /**
     * 时间
     */
    @Column(name = "_time", timestamp = true) // 映射到 _time 字段
    private Instant locationTime;

    /**
     * 原始值
     */
    @Column(name = "originalValue") // 映射到 originalValue 字段
    private double originalValue;

    /**
     * 实际值
     */
    @Column(name = "actualValue") // 映射到 actualValue 字段
    private double actualValue;
}