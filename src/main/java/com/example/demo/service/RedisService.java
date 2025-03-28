package com.example.demo.service;

import com.alibaba.fastjson.JSON;
import com.example.demo.config.RedisConfig;
import com.example.demo.entity.MonitorData;
import com.example.demo.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class RedisService {
    private static final String QUERY_PREFIX = "query:";
    private static final String QUERY_META_PREFIX = "query_meta:";
    private static final Duration EXPIRE_DURATION = Duration.ofHours(24); // 24小时

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedisConfig redisConfig;


    // 生成查询签名作为缓存key的一部分
    public String generateQuerySignature(String fields, long startTime, long stopTime) {
        return fields + "|" + startTime + "|" + stopTime;
    }
    public String getExistingQueryId(String querySignature) {
        return redisTemplate.opsForValue().get(QUERY_META_PREFIX + querySignature);
    }
    private void checkAndCleanRedisMemory() {
        try {
            // 获取Redis内存信息
            Properties memoryInfo = redisTemplate.getRequiredConnectionFactory()
                    .getConnection()
                    .info("memory");

            long usedMemory = Long.parseLong(memoryInfo.getProperty("used_memory"));
//            System.out.println("Memory used" + usedMemory);
//            System.out.println("Max Memory" + redisConfig.getMaxMemory());
            if (usedMemory > redisConfig.getMaxMemory()) {
                // 清理策略：删除最早的20%的key
                Set<String> keys = redisTemplate.keys(QUERY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    List<String> sortedKeys = new ArrayList<>(keys);
                    Collections.sort(sortedKeys); // 按创建时间排序

                    int keysToDelete = (int) (sortedKeys.size() * 0.2);
                    for (int i = 0; i < keysToDelete; i++) {
                        String metaKey = QUERY_META_PREFIX + sortedKeys.get(i).substring(QUERY_PREFIX.length());
                        redisTemplate.delete(sortedKeys.get(i));
                        redisTemplate.delete(metaKey);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.logOperation("000","Redis内存清理失败", String.valueOf(e));
        }
    }

    public String cacheQueryResult(String fields, long startTime, long stopTime, List<MonitorData> result) {
        this.checkAndCleanRedisMemory();

        String querySignature = generateQuerySignature(fields, startTime, stopTime);
        String existingQueryId = redisTemplate.opsForValue().get(QUERY_META_PREFIX + querySignature);

        if (existingQueryId != null) {
            // 使用 Duration 设置过期时间
            redisTemplate.expire(QUERY_PREFIX + existingQueryId, EXPIRE_DURATION);
            redisTemplate.expire(QUERY_META_PREFIX + querySignature, EXPIRE_DURATION);
            return existingQueryId;
        }

        String queryId = UUID.randomUUID().toString();
        String dataKey = QUERY_PREFIX + queryId;
        String metaKey = QUERY_META_PREFIX + querySignature;

        try {
            String jsonResult = JSON.toJSONString(result);
            // 使用带有过期时间的 set 方法
            redisTemplate.opsForValue().set(dataKey, jsonResult, EXPIRE_DURATION);
            redisTemplate.opsForValue().set(metaKey, queryId, EXPIRE_DURATION);
            return queryId;
        } catch (Exception e) {
            throw new RuntimeException("缓存查询结果失败", e);
        }
    }

    public List<MonitorData> getQueryResult(String queryId) {
        String key = QUERY_PREFIX + queryId;
        String jsonResult = redisTemplate.opsForValue().get(key);

        if (jsonResult == null) {
            return null;
        }

        try {
            // 使用 Duration 更新过期时间
            redisTemplate.expire(key, EXPIRE_DURATION);
            return JSON.parseArray(jsonResult, MonitorData.class);
        } catch (Exception e) {
            throw new RuntimeException("解析查询结果失败", e);
        }
    }
}