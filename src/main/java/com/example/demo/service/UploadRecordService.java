package com.example.demo.service;

import com.example.demo.entity.UploadRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UploadRecordService {
    private static final String UPLOAD_KEY_PREFIX = "upload:";

    @Autowired
    @Qualifier("uploadRecordRedisTemplate") // 注入专用模板
    private RedisTemplate<String, Object> redisTemplate;

    // 保存或更新记录
    public void saveRecord(String dataType, String filePath, String status) {
        // 对文件路径进行编码，替换特殊字符
        String encodedFilePath = filePath.replace(":", "::");
        String key = UPLOAD_KEY_PREFIX + dataType + ":" + encodedFilePath;

        Map<String, Object> record = new HashMap<>();
        record.put("status", status);
        record.put("timestamp", System.currentTimeMillis());
        redisTemplate.opsForHash().putAll(key, record);
        redisTemplate.expire(key, Duration.ofDays(30));
    }

    // 删除记录
    public void deleteRecord(String dataType, String filePath) {
        String encodedFilePath = filePath.replace(":", "::");
        String key = UPLOAD_KEY_PREFIX + dataType + ":" + encodedFilePath;
        redisTemplate.delete(key);
    }

    // 获取指定类型的所有记录
    public List<UploadRecord> getRecords(String dataType) {
        String pattern = UPLOAD_KEY_PREFIX + dataType + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        return redisTemplate.execute(connection -> {
            List<UploadRecord> records = new ArrayList<>();
            for (String key : keys) {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

                // 改进的key解析逻辑
                String prefixRemoved = key.substring(UPLOAD_KEY_PREFIX.length());
                int firstColon = prefixRemoved.indexOf(":");
                String actualDataType = prefixRemoved.substring(0, firstColon);
                String encodedFilePath = prefixRemoved.substring(firstColon + 1);
                String filePath = encodedFilePath.replace("::", ":");

                records.add(new UploadRecord(
                        actualDataType,
                        filePath,
                        (String) entries.get("status"),
                        (Long) entries.get("timestamp")
                ));
            }
            return records.stream()
                    .sorted((a,b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .collect(Collectors.toList());
        }, true);
    }
}