package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.MonitorData;
import com.example.demo.entity.WeatherData;
import com.example.demo.entity.WeightData;
import com.example.demo.service.DynamicWeighingService;
import com.example.demo.service.WeatherService;
import com.example.demo.util.DBUtilSearch;
import com.example.demo.util.LogUtil;
import com.influxdb.client.InfluxDBClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final InfluxDBClient influxDBClient;

    public SearchController(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @PostMapping("/high_sensor")
    public CommonResult<List<MonitorData>> HighSensorSearch(
            @RequestParam String fields,
            @RequestParam Long startTime,
            @RequestParam Long stopTime,
            @RequestParam String userId) {
        try {
            List<String> fieldList = Arrays.asList(fields.split(","));
            List<MonitorData> result = DBUtilSearch.BaseQuery(influxDBClient, fieldList, startTime, stopTime);

            // 记录查询操作日志
            LogUtil.logOperation(userId, "QUERY", "HighSensorSearch - Fields: " + fields + ", Start: " + startTime + ", Stop: " + stopTime);

            return CommonResult.success(result);
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
}