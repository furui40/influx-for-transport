package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.MonitorData;
import com.example.demo.util.DBUtilSearch;
import com.influxdb.client.InfluxDBClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/high_sensor")
public class MonitorController {

    private final InfluxDBClient influxDBClient;

    // 注入 InfluxDBClient
    public MonitorController(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    /**
     * 传感器数据查询接口
     *
     * @param fields    需要查询的字段列表（如 "1_Ch1_ori,2_Ch2_act"）
     * @param startTime 查询起始时间（时间戳，秒）
     * @param stopTime  查询结束时间（时间戳，秒）
     * @return 查询结果
     */
    @PostMapping("/search")
    public CommonResult<List<MonitorData>> search(
            @RequestParam String fields,
            @RequestParam Long startTime,
            @RequestParam Long stopTime) {
        try {
            // 将字段字符串转换为列表
            List<String> fieldList = Arrays.asList(fields.split(","));

            // 调用查询工具类
            List<MonitorData> result = DBUtilSearch.BaseQuery(influxDBClient, fieldList, startTime, stopTime);

            // 返回成功结果
            return CommonResult.success(result);
        } catch (Exception e) {
            // 返回错误信息
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }
}