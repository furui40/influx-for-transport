package com.example.demo.util;

import java.util.Date;
import java.util.Map;

public class MapUtil {

    // 从Map中安全地获取String类型的值
    public static String getStr(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    // 从Map中安全地获取Date类型的值
    public static Date getDate(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof Date ? (Date) value : null;
    }

    // 从Map中安全地获取Integer类型的值
    public static Integer getInt(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof Integer ? (Integer) value : null;
    }

    // 从Map中安全地获取Double类型的值
    public static Double getDouble(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof Double ? (Double) value : null;
    }

    // 从Map中安全地获取Long类型的值
    public static Long getLong(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof Long ? (Long) value : null;
    }

    // 从Map中安全地获取Boolean类型的值
    public static Boolean getBool(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
}
