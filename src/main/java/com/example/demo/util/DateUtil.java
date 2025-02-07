package com.example.demo.util;

import com.example.demo.entity.DateTime;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

    // 格式化 DateTime 对象
    public static String format(DateTime dateTime, String pattern) {
        return dateTime.getLocalDateTime().format(DateTimeFormatter.ofPattern(pattern));
    }

    // 解析字符串为 DateTime 对象
    public static DateTime parse(String dateStr) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(DatePattern.DEFAULT_PATTERN));
        return new DateTime(localDateTime);
    }

    // 时间偏移（小时）
    public static DateTime offsetHour(DateTime dateTime, int hours) {
        LocalDateTime localDateTime = dateTime.getLocalDateTime().plusHours(hours);
        return new DateTime(localDateTime);
    }

    // 时间偏移（秒）
    public static DateTime offsetSecond(DateTime dateTime, int seconds) {
        LocalDateTime localDateTime = dateTime.getLocalDateTime().plusSeconds(seconds);
        return new DateTime(localDateTime);
    }

    // 将 DateTime 转换为 Date
    public static Date date(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        // 将 DateTime 的 LocalDateTime 转换为 Date，使用东八区时区
        return Date.from(dateTime.getLocalDateTime()
                .atZone(ZoneId.of("Asia/Shanghai"))  // 强制转换为东八区时区
                .toInstant());
    }

    // 格式化时间间隔（秒）
    public static String formatBetween(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds % 60);
    }

    // 转换 LocalDateTime 为 UTC 时间
    public static String toUTC(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault())  // 系统默认时区
                .withZoneSameInstant(ZoneOffset.UTC)  // 转换为 UTC
                .format(DateTimeFormatter.ofPattern(DatePattern.UTC_PATTERN));
    }

    // 将 Date 转换为东八区的 LocalDateTime
    public static DateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        // 将 Date 转换为 Instant，再转换为东八区的 LocalDateTime
        LocalDateTime localDateTime = date.toInstant()
                .atZone(ZoneId.of("Asia/Shanghai"))  // 使用东八区时区
                .toLocalDateTime();
        return new DateTime(localDateTime);  // 返回封装了 LocalDateTime 的 DateTime 对象
    }

    // 格式化 Date 为字符串（默认格式：yyyy-MM-dd HH:mm:ss）
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DatePattern.DEFAULT_PATTERN);
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.of("Asia/Shanghai"))  // 强制转换为东八区时区
                .toLocalDateTime()
                .format(formatter);
    }

    // 获取时间中的小时部分（供其他用途）
//    public static Object hour(Date collectTime, boolean b) {
//        //TODO: 实现此功能
//        return null;
//    }
}
