package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true)
@Measurement(name = "weight_data")
public class WeightData {
    @Column(name = "id")
    private String id;

    @Column(name = "_time", timestamp = true)
    private Instant timestamp;

    @Column(name = "weight_kg")
    private double weightKg;

    @Column(name = "vehicle_length")
    private double vehicleLength;

    @Column(name = "lane")
    private String lane;

    @Column(name = "axle_count")
    private double axleCount;

    @Column(name = "speed")
    private double speed;

    @Column(name = "temperature")
    private double temperature;

    @Column(name = "direction")
    private String direction;

    @Column(name = "cross_lane")
    private String crossLane;

    @Column(name = "type")
    private String type;

    @Column(name = "axle_weight_1")
    private double axleWeight1;

    @Column(name = "axle_weight_2")
    private double axleWeight2;

    @Column(name = "axle_weight_3")
    private double axleWeight3;

    @Column(name = "axle_weight_4")
    private double axleWeight4;

    @Column(name = "axle_weight_5")
    private double axleWeight5;

    @Column(name = "axle_weight_6")
    private double axleWeight6;

    @Column(name = "wheelbase_1")
    private double wheelbase1;

    @Column(name = "wheelbase_2")
    private double wheelbase2;

    @Column(name = "wheelbase_3")
    private double wheelbase3;

    @Column(name = "wheelbase_4")
    private double wheelbase4;

    @Column(name = "wheelbase_5")
    private double wheelbase5;

    @Column(name = "vehicle_type_code")
    private String vehicleTypeCode;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "axle_1_kn")
    private double axle1Kn;

    @Column(name = "axle_2_kn")
    private double axle2Kn;

    @Column(name = "axle_3_kn")
    private double axle3Kn;

    @Column(name = "offset")
    private double offset;

}