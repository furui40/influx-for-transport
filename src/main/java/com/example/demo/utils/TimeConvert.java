package com.example.demo.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeConvert {
    public static Long parseDateTimeToTimestamp(String dateTimeStr) throws ParseException {
        // 支持多种前端可能传入的格式
        String[] patterns = {
                "yyyy/MM/ddHH:mm:ss",  // 如 "2024/07/1212:33:11"
                "yyyy-MM-dd HH:mm:ss",  // 如 "2024-07-12 12:33:11"
                "yyyyMMddHHmmss"        // 如 "20240712123311"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                Date date = sdf.parse(dateTimeStr);
                return date.getTime() / 1000; // 转换为秒级时间戳
            } catch (ParseException e) {
            }
        }
        throw new ParseException("无法解析的时间格式: " + dateTimeStr, 0);
    }
}
