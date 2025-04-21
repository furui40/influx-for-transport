package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.DownloadApply;
import com.example.demo.entity.MonitorData;
import com.example.demo.service.DownloadService;
import com.example.demo.service.HighSensorService;
import com.example.demo.utils.LogUtil;
import com.example.demo.utils.TimeConvert;
import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/download")
public class DownloadController {
    private final InfluxDBClient influxDBClient;
    private final DownloadService downloadService;

    @Value("${data.download-dir:/data}")
    private String downloadDir;

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

    @PostMapping("/startdownload")
    public CommonResult startDownload(
            @RequestParam String fields,
            @RequestParam String startTimeStr,
            @RequestParam String stopTimeStr,
            @RequestParam String userId,
            @RequestParam Long samplingInterval,
            @RequestParam String applyId) {

        try {
            Long startTime = TimeConvert.parseDateTimeToTimestamp(startTimeStr);
            Long stopTime = TimeConvert.parseDateTimeToTimestamp(stopTimeStr);

            List<String> fieldList = Arrays.asList(fields.split(","));

            //创建下载目录
            String applyDir = downloadDir + File.separator + applyId;
            File dir = new File(applyDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            //分割时间范围
            final long MAX_QUERY_INTERVAL = 60; // 1分钟
            long currentStart = startTime;
            int fileCount = 0;

            while (currentStart < stopTime) {
                long currentStop = Math.min(currentStart + MAX_QUERY_INTERVAL, stopTime);

                List<MonitorData> monitorDataList = HighSensorService.queryData(
                        influxDBClient,
                        fieldList,
                        currentStart,
                        currentStop,
                        samplingInterval
                );

                if (monitorDataList != null && !monitorDataList.isEmpty()) {
                    // 写入文件
                    String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss")
                            .format(new Date(currentStart * 1000)) + ".xlsx";
                    String filePath = applyDir + File.separator + fileName;

                    downloadService.writeDataToFile(monitorDataList, applyId, filePath);
                    fileCount++;
                }

                currentStart = currentStop;
            }

            // 创建完成标记文件
            File completeFlag = new File(applyDir + File.separator + "下载已完成");
            completeFlag.createNewFile();

            // 更新申请状态为"处理完成"
            DownloadApply apply = new DownloadApply()
                    .setApplyId(applyId)
                    .setStatus("下载完成")
                    .setMsg("数据已准备好，共生成" + fileCount + "个文件");
            downloadService.updateApplyStatus(influxDBClient, apply);

            return CommonResult.success(null, "下载任务已开始处理");

        } catch (Exception e) {
            // 记录错误日志
            LogUtil.logOperation(userId, "DOWNLOAD", "StartDownload failed: " + e.getMessage());

            // 更新申请状态为"处理失败"
            try {
                DownloadApply apply = new DownloadApply()
                        .setApplyId(applyId)
                        .setStatus("下载失败")
                        .setMsg("下载处理失败: " + e.getMessage());
                downloadService.updateApplyStatus(influxDBClient, apply);
            } catch (Exception ex) {
                LogUtil.logOperation(userId, "DOWNLOAD", "Update status failed: " + ex.getMessage());
            }

            return CommonResult.failed("下载处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/send")
    public CommonResult sendDownloadLink(
            @RequestParam String applyId,
            @RequestParam String downloadLink) {
        try {
            return downloadService.sendDownloadLink(influxDBClient, applyId, downloadLink);
        } catch (Exception e) {
            LogUtil.logOperation(applyId, "SEND_LINK", "Controller发送链接失败: " + e.getMessage());
            return CommonResult.failed("发送链接失败: " + e.getMessage());
        }
    }

}
