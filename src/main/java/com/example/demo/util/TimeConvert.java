package com.example.demo.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeConvert {
    public static Instant toLocalTime(Instant originalTime) {
        // 将原始UTC时间转换为东八区时间 (中国标准时间)
        System.out.println(originalTime);
        ZonedDateTime zonedDateTime = originalTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Shanghai"));
        System.out.println(zonedDateTime);
        System.out.println(zonedDateTime.toInstant());
        // 返回转换后的时间
        return zonedDateTime.toInstant();
    }}
