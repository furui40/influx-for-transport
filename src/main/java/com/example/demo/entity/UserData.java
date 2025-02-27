package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true)
@Measurement(name = "user_data")
public class UserData {

    /**
     * 用户ID
     */
    @Column(name = "user_id", tag = true, timestamp = true)
    private String userId;

    /**
     * 用户名
     */
    @Column(name = "user_name")
    private String userName;

    /**
     * 密码
     */
    @Column(name = "_value")
    private String password;

    /**
     * 用户状态（例如：admin,normal等）
     */
    @Column(name = "user_status")
    private String userStatus;

    @Column(name = "_time")
    private Instant timestamp;
}
