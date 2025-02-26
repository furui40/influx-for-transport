package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true)
@Measurement(name = "apply_data") // 指定 InfluxDB 中的 measurement 名称
public class DownloadApply {
    @Column(name = "apply_id", tag = true, timestamp = true) // apply_id 作为 tag
    private String applyId; // 申请ID，值为当前纳秒时间戳

    @Column(name = "status", tag = true) // status 作为 tag
    private String status; // 当前状态，分为“已申请”、“审核通过”、“审核不通过”、“已完成”

    @Column(name = "data_type") // data_type 作为 field
    private String dataType; // 数据类型

    @Column(name = "fields") // fields 作为 field
    private String fields; // 数据项名称

    @Column(name = "start_time") // start_time 作为 field
    private String startTime; // 开始时间（字符串格式）

    @Column(name = "stop_time") // stop_time 作为 field
    private String stopTime; // 结束时间（字符串格式）

    @Column(name = "user_email") // user_email 作为 field
    private String userEmail; // 用户邮箱

    @Column(name = "user_id", tag = true) // user_id 作为 tag
    private String userId; // 用户ID

    @Column(name = "msg") // msg 作为 field
    private String msg; // 用户ID

    @Column(name = "_time") // msg 作为 field
    private Instant timestamp; // 用户ID

    public DownloadApply() {
        this.status = "已申请"; // 默认状态为“已申请”
    }
}