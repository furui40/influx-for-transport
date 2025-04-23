//package com.example.demo.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.Map;
//
//@Service
//public class HighSensorRecordService {
//    private static final String SENSOR_KEY_PREFIX = "sensor:";
//    private static final String SENSOR_DATA_KEY = "sensor_data";
//
//    @Autowired
//    @Qualifier("highSensorRedisTemplate")
//    private RedisTemplate<String, Object> redisTemplate;
//
//    // 保存传感器数据
//    public void saveSensorData(String sensorId, Map<String, Object> sensorData) {
//        String key = SENSOR_KEY_PREFIX + sensorId;
//        redisTemplate.opsForHash().putAll(key, sensorData);
//        redisTemplate.expire(key, Duration.ofSeconds(redisConfig.getHighSensor().getExpireTime()));
//
//        // 同时保存到全局集合
//        redisTemplate.opsForSet().add(SENSOR_DATA_KEY, sensorId);
//    }
//
//    // 批量保存传感器数据
//    public void batchSaveSensorData(Map<String, Map<String, Object>> sensorDataMap) {
//        sensorDataMap.forEach((sensorId, data) -> {
//            String key = SENSOR_KEY_PREFIX + sensorId;
//            redisTemplate.opsForHash().putAll(key, data);
//            redisTemplate.expire(key, Duration.ofSeconds(redisConfig.getHighSensor().getExpireTime()));
//            redisTemplate.opsForSet().add(SENSOR_DATA_KEY, sensorId);
//        });
//    }
//
//    // 获取单个传感器数据
//    public Map<String, Object> getSensorData(String sensorId) {
//        String key = SENSOR_KEY_PREFIX + sensorId;
//        return redisTemplate.opsForHash().entries(key);
//    }
//
//    // 获取所有传感器ID
//    public Set<String> getAllSensorIds() {
//        return redisTemplate.opsForSet().members(SENSOR_DATA_KEY).stream()
//                .map(Object::toString)
//                .collect(Collectors.toSet());
//    }
//
//    // 获取所有传感器数据
//    public Map<String, Map<String, Object>> getAllSensorData() {
//        Set<String> sensorIds = getAllSensorIds();
//        Map<String, Map<String, Object>> result = new HashMap<>();
//
//        sensorIds.forEach(sensorId -> {
//            result.put(sensorId, getSensorData(sensorId));
//        });
//
//        return result;
//    }
//
//    // 删除传感器数据
//    public void deleteSensorData(String sensorId) {
//        String key = SENSOR_KEY_PREFIX + sensorId;
//        redisTemplate.delete(key);
//        redisTemplate.opsForSet().remove(SENSOR_DATA_KEY, sensorId);
//    }
//
//    // 获取传感器数量
//    public Long getSensorCount() {
//        return redisTemplate.opsForSet().size(SENSOR_DATA_KEY);
//    }
//}