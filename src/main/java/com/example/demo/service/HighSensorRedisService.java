package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HighSensorRedisService {
    private static final String BATCH_KEY = "high_sensor:batches";
    private static final String SUCCESS_SUFFIX = ":success";
    private static final String FAILED_SUFFIX = ":failed";

    @Autowired
    @Qualifier("highSensorRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    // 记录批次开始
    public void startBatch(String batchId) {
        redisTemplate.opsForHash().put(BATCH_KEY, batchId,
                "started:" + System.currentTimeMillis());
    }

    // 标记批次成功
    public void markBatchSuccess(String batchId) {
        redisTemplate.opsForHash().put(BATCH_KEY, batchId,
                "success:" + System.currentTimeMillis());
        redisTemplate.opsForSet().add(BATCH_KEY + SUCCESS_SUFFIX, batchId);
        redisTemplate.opsForSet().remove(BATCH_KEY + FAILED_SUFFIX, batchId);
    }

    // 标记批次失败
    public void markBatchFailed(String batchId, String error) {
        redisTemplate.opsForHash().put(BATCH_KEY, batchId,
                "failed:" + System.currentTimeMillis() + "|" + error);
        redisTemplate.opsForSet().add(BATCH_KEY + FAILED_SUFFIX, batchId);
        redisTemplate.opsForSet().remove(BATCH_KEY + SUCCESS_SUFFIX, batchId);
    }

    // 获取批次详情（类型安全转换）
    public Map<String, String> getBatchDetails() {
        return redisTemplate.<String, Object>opsForHash()
                .entries(BATCH_KEY)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                ));
    }

    // 获取成功批次（带类型转换）
    public Set<String> getSuccessBatches() {
        return Optional.ofNullable(redisTemplate.opsForSet().members(BATCH_KEY + SUCCESS_SUFFIX))
                .orElse(Collections.emptySet())
                .stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    // 获取失败批次（带类型转换）
    public Set<String> getFailedBatches() {
        return Optional.ofNullable(redisTemplate.opsForSet().members(BATCH_KEY + FAILED_SUFFIX))
                .orElse(Collections.emptySet())
                .stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}