package com.example.demo.entity;

import java.time.LocalDateTime;

public class DateTime {
    private LocalDateTime localDateTime;

    public DateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public static DateTime now() {
        return new DateTime(LocalDateTime.now());
    }

    // 其他可以根据需要添加的日期时间操作方法
}