package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.JinMaData;
import com.example.demo.entity.MonitorData;
import com.example.demo.entity.WeatherData;
import com.example.demo.entity.WeightData;
import com.example.demo.service.DynamicWeighingService;
import com.example.demo.service.JinMaDataService;
import com.example.demo.service.RedisService;
import com.example.demo.service.WeatherService;
import com.example.demo.utils.DBUtilSearch;
import com.example.demo.utils.LogUtil;
import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {


    private final InfluxDBClient influxDBClient;
    private final JinMaDataService jinMaDataService;
    private final RedisService redisService;


    @PostMapping("/high_sensor")
    public CommonResult<String> highSensorSearch(
            @RequestParam String fields,
            @RequestParam Long startTime,
            @RequestParam Long stopTime,
            @RequestParam String userId,
            @RequestParam Long samplingInterval) {

        try {
            // 先尝试从Redis获取
            String querySignature = redisService.generateQuerySignature(fields, startTime, stopTime);
            String existingQueryId = redisService.getExistingQueryId(querySignature);

            if (existingQueryId != null) {
                System.out.println("缓存命中");
                LogUtil.logOperation(userId, "CACHE", "Reuse cached query: " + existingQueryId);
                return CommonResult.success(existingQueryId);
            }
            System.out.println("缓存未命中");
            // 需要查询数据库
            List<String> fieldList = Arrays.asList(fields.split(","));
            List<MonitorData> result = DBUtilSearch.BaseQuery(influxDBClient, fieldList,
                    startTime, stopTime, samplingInterval);

            // 缓存结果
            String queryId = redisService.cacheQueryResult(fields, startTime, stopTime, result);

            LogUtil.logOperation(userId, "QUERY", "HighSensorSearch - Fields: " + fields +
                    ", Start: " + startTime + ", Stop: " + stopTime);

            return CommonResult.success(queryId);
        } catch (Exception e) {
            LogUtil.logOperation(userId, "QUERY", "HighSensorSearch failed: " + e.getMessage());
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/dynamicWeighing")
    public CommonResult<List<WeightData>> DynamicWeighingSearch(
            @RequestParam Long startTime,
            @RequestParam Long stopTime,
            @RequestParam String userId) {
        DynamicWeighingService dynamicWeighingService = new DynamicWeighingService();
        try {
            List<WeightData> result = dynamicWeighingService.queryWeightData(influxDBClient, startTime, stopTime);

            // 记录查询操作日志
            LogUtil.logOperation(userId, "QUERY", "DynamicWeighingSearch - Start: " + startTime + ", Stop: " + stopTime);

            return CommonResult.success(result);
        } catch (Exception e) {
            LogUtil.logOperation(userId, "QUERY", "DynamicWeighingSearch failed: " + e.getMessage());
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/weather")
    public CommonResult<List<WeatherData>> WeatherSearch(
            @RequestParam Long startTime,
            @RequestParam Long stopTime,
            @RequestParam String userId) {
        WeatherService weatherService = new WeatherService();
        try {
            List<WeatherData> result = weatherService.queryWeatherData(influxDBClient, startTime, stopTime);

            // 记录查询操作日志
            LogUtil.logOperation(userId, "QUERY", "WeatherSearch - Start: " + startTime + ", Stop: " + stopTime);

            return CommonResult.success(result);
        } catch (Exception e) {
            LogUtil.logOperation(userId, "QUERY", "WeatherSearch failed: " + e.getMessage());
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/subside")
    public CommonResult<List<JinMaData>> SubsideSearch(
            @RequestParam String fields,
            @RequestParam Long startTime,
            @RequestParam Long stopTime,
            @RequestParam String userId) {
        try {
            List<String> fieldList = Arrays.asList(fields.split(","));
            List<JinMaData> result = jinMaDataService.queryJinMaData(influxDBClient,startTime, stopTime,fieldList, "subside");

            // 记录查询操作日志
            LogUtil.logOperation(userId, "QUERY", "SubsideSearch - Start: " + startTime + ", Stop: " + stopTime);

            return CommonResult.success(result);
        } catch (Exception e) {
            LogUtil.logOperation(userId, "QUERY", "SubsideSearch failed: " + e.getMessage());
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/waterPressure")
    public CommonResult<List<JinMaData>> WaterPressureSearch(
            @RequestParam String fields,
            @RequestParam Long startTime,
            @RequestParam Long stopTime,
            @RequestParam String userId) {
        try {
            List<String> fieldList = Arrays.asList(fields.split(","));
            List<JinMaData> result = jinMaDataService.queryJinMaData(influxDBClient,startTime, stopTime,fieldList, "waterPressure");

            // 记录查询操作日志
            LogUtil.logOperation(userId, "QUERY", "WaterPressureSearch - Start: " + startTime + ", Stop: " + stopTime);

            return CommonResult.success(result);
        } catch (Exception e) {
            LogUtil.logOperation(userId, "QUERY", "WaterPressureSearch failed: " + e.getMessage());
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/humiture")
    public CommonResult<List<JinMaData>> HumitureSearch(
            @RequestParam String fields,
            @RequestParam Long startTime,
            @RequestParam Long stopTime,
            @RequestParam String userId) {
        try {
            List<String> fieldList = Arrays.asList(fields.split(","));
            List<JinMaData> result = jinMaDataService.queryJinMaData(influxDBClient,startTime, stopTime,fieldList, "humiture");

            // 记录查询操作日志
            LogUtil.logOperation(userId, "QUERY", "HumitureSearch - Start: " + startTime + ", Stop: " + stopTime);

            return CommonResult.success(result);
        } catch (Exception e) {
            LogUtil.logOperation(userId, "QUERY", "HumitureSearch failed: " + e.getMessage());
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/query_result/{queryId}")
    public CommonResult<List<MonitorData>> getQueryResult(
            @PathVariable String queryId,
            @RequestParam String userId) {

        try {
            List<MonitorData> result = redisService.getQueryResult(queryId);

            if (result == null) {
                LogUtil.logOperation(userId, "CACHE", "Query result not found: " + queryId);
                return CommonResult.failed("该查询编号不存在或者该查询结果已过期");
            }

            LogUtil.logOperation(userId, "FETCH", "Fetched query result: " + queryId);
            return CommonResult.success(result);
        } catch (Exception e) {
            LogUtil.logOperation(userId, "FETCH", "Failed to fetch query result: " + e.getMessage());
            return CommonResult.failed("获取查询结果失败: " + e.getMessage());
        }
    }
}