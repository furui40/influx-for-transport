package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Accessors(chain = true)
@Measurement(name = "sensor_data")
public class MonitorData {

    /**
     * 解调器id
     */
    @Column(name = "decoder_id",tag = true)
    private String decoderId;

    /**
     * 信道id
     */
    @Column(name = "channel_id",tag = true)
    private String channelId;

    /**
     * 时间
     */
    @Column(timestamp = true)
    private Instant locationTime;

    /**
     * 原始值
     */
    @Column(name = "originalValue")
    private double originalValue;

    /**
     * 实际值
     */
    @Column(name = "actualValue")
    private double actualValue;

}
