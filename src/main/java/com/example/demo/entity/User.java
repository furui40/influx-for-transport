package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Measurement(name = "user_data")
public class User {

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
    @Column(name = "password")
    private String password;

    /**
     * 用户状态（例如：active, inactive等）
     */
    @Column(name = "user_status")
    private String userStatus;

}
