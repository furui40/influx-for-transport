package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.DownloadApply;
import com.example.demo.service.DownloadService;
import com.example.demo.util.LogUtil;
import com.influxdb.client.InfluxDBClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/download")
public class DownloadController {
    private final InfluxDBClient influxDBClient;
    private final DownloadService downloadService;

    // 通过构造函数注入 InfluxDBClient 和 DownloadService
    public DownloadController(InfluxDBClient influxDBClient, DownloadService downloadService) {
        this.influxDBClient = influxDBClient;
        this.downloadService = downloadService;
    }

    @PostMapping("/newapply")
    public CommonResult<DownloadApply> newApply(
            @RequestParam String dataType,
            @RequestParam String fields,
            @RequestParam Long startTime,
            @RequestParam Long stopTime,
            @RequestParam String userEmail,
            @RequestParam String userId) {
        try {
            // 创建 DownloadApply 对象
            DownloadApply apply = new DownloadApply()
                    .setApplyId(String.valueOf(Instant.now().toEpochMilli()))
                    .setDataType(dataType)
                    .setFields(fields)
                    .setStartTime(String.valueOf(startTime))
                    .setStopTime(String.valueOf(stopTime))
                    .setUserEmail(userEmail)
                    .setUserId(userId);

            // 调用 DownloadService 的 addNewApply 方法
            CommonResult<DownloadApply> result = downloadService.addNewApply(influxDBClient, apply);

            // 返回结果
            return result;
        } catch (Exception e) {
            // 记录错误日志
            LogUtil.logOperation(userId, "APPLY", "NewApply failed: " + e.getMessage());

            // 返回失败响应
            return CommonResult.failed("申请提交失败: " + e.getMessage());
        }
    }

    @PostMapping("/searchapply")
    public CommonResult<?> searchApply(
            @RequestParam String method,
            @RequestParam String userId) {
        try {
            // 调用 DownloadService 的查询方法
            return downloadService.searchApply(influxDBClient, method, userId);
        } catch (Exception e) {
            // 记录错误日志
            LogUtil.logOperation(userId, "SEARCH", "SearchApply failed: " + e.getMessage());

            // 返回失败响应
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/passapply")
    public CommonResult passApply(
            @RequestParam String applyIds){
        List<String> applyIdList = Arrays.asList(applyIds.split(","));
        for(String applyId: applyIdList){
            try{
                downloadService.passApply(influxDBClient,applyId);
            }catch (Exception e) {
                // 记录错误日志
                LogUtil.logOperation(applyId, "PASS", "PassApply failed: " + e.getMessage());

                // 返回失败响应
                return CommonResult.failed("审核通过失败: " + e.getMessage());
            }
        }
        return CommonResult.success(applyIdList,"更改成功");
    }

    @PostMapping("/rejectapply")
    public CommonResult rejectApply(
            @RequestParam String applyIds,
            @RequestParam String reason){
        List<String> applyIdList = Arrays.asList(applyIds.split(","));
        for(String applyId: applyIdList){
            try{
                downloadService.rejectApply(influxDBClient,applyId,reason);
            }catch (Exception e) {
                // 记录错误日志
                LogUtil.logOperation(applyId, "REJECT", "RejectApply failed: " + e.getMessage());

                // 返回失败响应
                return CommonResult.failed("审核拒绝失败: " + e.getMessage());
            }
        }
        return CommonResult.success(applyIdList,"更改成功");
    }
}
