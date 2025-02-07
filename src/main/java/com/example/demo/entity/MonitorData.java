package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Accessors(chain = true)
@Measurement(name = "device_history_location")
public class MonitorData {

    /**
     * 设备id
     */
    @Column(name = "device_id",tag = true)
    private String deviceId;

    /**
     * 车辆id
     */
    @Column(name = "vehicle_id")
    private String vehicleId;

    /**
     * 位置时间
     */
    @Column(timestamp = true)
    private Instant locationTime;

    /**
     * 经度
     */
    @Column(name = "longitude")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Column(name = "latitude")
    private BigDecimal latitude;

}
