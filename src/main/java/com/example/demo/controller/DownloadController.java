package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.*;
import com.example.demo.service.*;
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
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/download")
public class DownloadController {
    private final InfluxDBClient influxDBClient;
    private final DownloadService downloadService;

    private final DynamicWeighingService dynamicWeighingService;

    private final WeatherService weatherService;

    private final JinMaDataService jinMaDataService;

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


    //分割时间范围
//            final long MAX_QUERY_INTERVAL = 15; //
//            long currentStart = startTime;
//            int fileCount = 0;
//
//            while (currentStart < stopTime) {
//                long currentStop = Math.min(currentStart + MAX_QUERY_INTERVAL, stopTime);
//
//                List<MonitorData> monitorDataList = HighSensorService.queryData(
//                        influxDBClient,
//                        fieldList,
//                        currentStart,
//                        currentStop,
//                        samplingInterval
//                );
//
//                if (monitorDataList != null && !monitorDataList.isEmpty()) {
//                    // 写入文件
//                    String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss")
//                            .format(new Date(currentStart * 1000)) + ".xlsx";
//                    String filePath = applyDir + File.separator + fileName;
//
//                    downloadService.writeHighSensorDataToFile(monitorDataList, filePath);
//                    fileCount++;
//                }
//
//                currentStart = currentStop;
//            }
    @PostMapping("/startdownload")
    public CommonResult startDownload(
            @RequestParam String fields,
            @RequestParam String startTimeStr,
            @RequestParam String stopTimeStr,
            @RequestParam String userId,
            @RequestParam Long samplingInterval,
            @RequestParam String applyId) {

        try {
            Long start = System.currentTimeMillis();

            Long startTime = TimeConvert.parseDateTimeToTimestamp(startTimeStr);
            Long stopTime = TimeConvert.parseDateTimeToTimestamp(stopTimeStr);

            List<String> fieldList = Arrays.asList(fields.split(","));

            //创建下载目录
            String applyDir = downloadDir + File.separator + applyId;
            File dir = new File(applyDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            downloadService.findAndUpdateApply(
                    influxDBClient,
                    applyId,
                    "下载中",
                    "正在下载",
                    null,
                    null
            );

            // 新实现：分批次多线程查询
            final long BATCH_DURATION = 30 * 60; // 每轮最大查询30分钟(1800秒)
            final long SEGMENT_DURATION = 15;     // 每个时间片15秒

            long currentBatchStart = startTime;
            int totalFileCount = 0;

            while (currentBatchStart < stopTime) {
                long currentBatchEnd = Math.min(currentBatchStart + BATCH_DURATION, stopTime);

                // 创建本批次的任务列表(按15秒分割)
                List<HighSensorService.QueryTask> batchTasks = new ArrayList<>();
                long segmentStart = currentBatchStart;

                while (segmentStart < currentBatchEnd) {
                    long segmentEnd = Math.min(segmentStart + SEGMENT_DURATION, currentBatchEnd);

                    String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss")
                            .format(new Date(segmentStart * 1000)) + ".xlsx";

                    batchTasks.add(new HighSensorService.QueryTask(
                            new HighSensorService.TimeRange(segmentStart, segmentEnd),
                            fileName
                    ));

                    segmentStart = segmentEnd;
                }

                // 执行本批次并发查询
                int batchFileCount = HighSensorService.concurrentQueryAndWrite(
                        influxDBClient,
                        fieldList,
                        batchTasks,
                        samplingInterval,
                        applyDir,
                        downloadService
                );

                totalFileCount += batchFileCount;

                currentBatchStart = currentBatchEnd;
            }

            // 创建完成标记文件
            File completeFlag = new File(applyDir + File.separator + "下载已完成");
            completeFlag.createNewFile();

            // 更新申请状态为"处理完成"
            DownloadApply apply = new DownloadApply()
                    .setApplyId(applyId)
                    .setStatus("下载完成")
                    .setMsg("数据已准备好，共生成" + totalFileCount + "个文件");
            downloadService.updateApplyStatus(influxDBClient, apply);

            System.out.println("查询下载用时： " + (System.currentTimeMillis() - start)/1000.0 + "秒");
            return CommonResult.success(null, "下载任务已完成");

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

    @PostMapping("/startdownload2")
    public CommonResult startDownload2(
            @RequestParam String fields,
            @RequestParam String startTimeStr,
            @RequestParam String stopTimeStr,
            @RequestParam String userId,
            @RequestParam Long samplingInterval,
            @RequestParam String applyId) {

        try {
            Long start = System.currentTimeMillis();

            Long startTime = TimeConvert.parseDateTimeToTimestamp(startTimeStr);
            Long stopTime = TimeConvert.parseDateTimeToTimestamp(stopTimeStr);
            List<String> fieldList = Arrays.asList(fields.split(","));

            String applyDir = downloadDir + File.separator + applyId;
            File dir = new File(applyDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            downloadService.findAndUpdateApply(
                    influxDBClient,
                    applyId,
                    "下载中",
                    "正在下载",
                    null,
                    null
            );

            // 先查询动态称重数据
            List<WeightData> weightDataList = dynamicWeighingService.queryWeightData(influxDBClient, startTime, stopTime);

            // 保存完整的称重数据
            if (weightDataList != null && !weightDataList.isEmpty()) {
                String weightFilePath = applyDir + File.separator + "动态称重.xlsx";
                downloadService.writeWeightDataToFile(weightDataList, weightFilePath);
            }



//            // 对每个称重时间点查询前后6秒的高频数据
//            int fileCount = 0;
//            for (WeightData weightData : weightDataList) {
//                long weightTime = weightData.getTimestamp().getEpochSecond();
//                long queryStart = weightTime - 6;  // 前6秒
//                long queryEnd = weightTime + 6;    // 后6秒
//
//                // 确保查询时间在请求的时间范围内
//                queryStart = Math.max(queryStart, startTime);
//                queryEnd = Math.min(queryEnd, stopTime);
//
//                List<MonitorData> monitorDataList = HighSensorService.queryData(
//                        influxDBClient,
//                        fieldList,
//                        queryStart,
//                        queryEnd,
//                        samplingInterval
//                );
//
//                if (monitorDataList != null && !monitorDataList.isEmpty()) {
//                    // 写入文件，文件名包含称重时间戳
//                    Instant weightInstant = weightData.getTimestamp();
//                    ZonedDateTime zonedDateTime = weightInstant.atZone(ZoneId.of("Asia/Shanghai"));
//                    String fileName = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
//                    String filePath = applyDir + File.separator + fileName;
//
//                    downloadService.writeHighSensorDataToFile(monitorDataList, filePath);
//                    fileCount++;
//                }
//            }

            // 生成高频数据查询任务
            List<HighSensorService.QueryTask> tasks = new ArrayList<>();
            for (WeightData weightData : weightDataList) {
                long weightTime = weightData.getTimestamp().getEpochSecond();
                long queryStart = Math.max(weightTime - 6, startTime);
                long queryEnd = Math.min(weightTime + 6, stopTime);

                // 生成文件名（使用称重时间戳）
                Instant weightInstant = weightData.getTimestamp();
                ZonedDateTime zdt = weightInstant.atZone(ZoneId.of("Asia/Shanghai"));
                String fileName = zdt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

                tasks.add(new HighSensorService.QueryTask(
                        new HighSensorService.TimeRange(queryStart, queryEnd),
                        fileName
                ));
            }

            // 执行并发查询
            int fileCount = HighSensorService.concurrentQueryAndWrite2(
                    influxDBClient,
                    fieldList,
                    tasks,
                    samplingInterval,
                    applyDir,
                    downloadService
            );

            // 创建完成标记文件
            File completeFlag = new File(applyDir + File.separator + "下载已完成");
            completeFlag.createNewFile();

            // 更新申请状态为"处理完成"
            DownloadApply apply = new DownloadApply()
                    .setApplyId(applyId)
                    .setStatus("下载完成")
                    .setMsg("数据已准备好，共生成" + fileCount + "个文件");
            downloadService.updateApplyStatus(influxDBClient, apply);

            System.out.println("查询下载用时： " + (System.currentTimeMillis() - start)/1000.0 + "秒");

            return CommonResult.success(null, "下载任务已完成");

        } catch (Exception e) {
            // 记录错误日志
            LogUtil.logOperation(userId, "DOWNLOAD2", "StartDownload2 failed: " + e.getMessage());

            // 更新申请状态为"处理失败"
            try {
                DownloadApply apply = new DownloadApply()
                        .setApplyId(applyId)
                        .setStatus("下载失败")
                        .setMsg("下载处理失败: " + e.getMessage());
                downloadService.updateApplyStatus(influxDBClient, apply);
            } catch (Exception ex) {
                LogUtil.logOperation(userId, "DOWNLOAD2", "Update status failed: " + ex.getMessage());
            }
            return CommonResult.failed("下载处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/startdownload3")
    public CommonResult startDownload3(
            @RequestParam String fields,
            @RequestParam String startTimeStr,
            @RequestParam String stopTimeStr,
            @RequestParam String userId,
            @RequestParam Long samplingInterval,
            @RequestParam String applyId) {

        try {
            Long start = System.currentTimeMillis();

            Long startTime = TimeConvert.parseDateTimeToTimestamp(startTimeStr);
            Long stopTime = TimeConvert.parseDateTimeToTimestamp(stopTimeStr);
            List<String> fieldList = Arrays.asList(fields.split(","));

            String applyDir = downloadDir + File.separator + applyId;
            File dir = new File(applyDir);
            if (!dir.exists()) dir.mkdirs();

            // 初始化各数据集合
            List<WeatherData> weatherDataList = new ArrayList<>();
            List<JinMaData> subsideDataList = new ArrayList<>();
            List<JinMaData> waterPressureDataList = new ArrayList<>();
            List<JinMaData> humitureDataList = new ArrayList<>();

            // 查询动态称重数据
            List<WeightData> weightDataList = dynamicWeighingService.queryWeightData(influxDBClient, startTime, stopTime);

            // 保存完整的称重数据
            if (weightDataList != null && !weightDataList.isEmpty()) {
                String weightFilePath = applyDir + File.separator + "动态称重.xlsx";
                downloadService.writeWeightDataToFile(weightDataList, weightFilePath);
            }

            int fileCount = 1;

            List<HighSensorService.QueryTask> tasks = new ArrayList<>();
            for (WeightData weightData : weightDataList) {
                long weightTime = weightData.getTimestamp().getEpochSecond();
                long queryStart = Math.max(weightTime - 6, startTime);
                long queryEnd = Math.min(weightTime + 6, stopTime);

                // 生成文件名（使用称重时间戳）
                Instant weightInstant = weightData.getTimestamp();
                ZonedDateTime zdt = weightInstant.atZone(ZoneId.of("Asia/Shanghai"));
                String fileName = zdt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

                tasks.add(new HighSensorService.QueryTask(
                        new HighSensorService.TimeRange(queryStart, queryEnd),
                        fileName
                ));
            }

            // 执行并发查询
            fileCount += HighSensorService.concurrentQueryAndWrite2(
                    influxDBClient,
                    fieldList,
                    tasks,
                    samplingInterval,
                    applyDir,
                    downloadService
            );

            // 处理每个称重点
            for (WeightData weightData : weightDataList) {
                long weightTime = weightData.getTimestamp().getEpochSecond();

                // 计算扩展时间范围（±301秒）
                long extendedStart = Math.max(weightTime - 301, startTime);
                long extendedEnd = Math.min(weightTime + 301, stopTime);

                // 查询并合并气象数据
                List<WeatherData> weatherResults = weatherService.queryWeatherData(
                        influxDBClient, extendedStart, extendedEnd
                );
                if (!weatherResults.isEmpty()) {
                    WeatherData weatherDataItem = weatherResults.get(0);
                    weatherDataItem.setTimestamp(weightData.getTimestamp()); // 修改时间戳
                    weatherDataList.add(weatherDataItem);
                }

                // 查询并合并金码数据（三组不同参数）
                String subside = "0034230033-01,0034230033-02,0034230033-03,0034230033-04,0034230033-05,0034230033-06,0034230033-07,0034230033-08,0034230033-09,0034230033-10,0034230033-11,0034230033-12,0034230033-13,0034230033-14,0034230033-15";
                String waterPressure = "0034230583-01,0034230583-02,0034230583-03,0034230583-04,0034230583-05,0034230583-06,0034230583-07,0034230583-08,0034230583-09,0034230610-01,0034230610-02,0034230610-03,0034230610-04,0034230610-05,0034230610-06,0034230610-07,0034230610-08,0034230610-09,0034230610-10,0034230610-11,0034230610-12,0034230610-13,0034230610-14,0034230610-15,0034230610-16,0034230610-17,0034230610-18,0034230610-19,0034230610-20";
                String humiture = "0034230034-01,0034230034-02,0034230034-03,0034230034-04,0034230034-05,0034230034-06,0034230034-07,0034230034-08,0034230034-09,0034230034-10,0034230034-11,0034230034-12,0034230034-13,0034230034-14,0034230034-15,0034230034-16,0034230583-10,0034230583-11,0034230583-12,0034230583-13,0034230583-14,0034230583-15,0034230583-16,0034230583-17,0034230583-18,0034230607-01,0034230607-02,0034230607-03,0034230607-04,0034230607-05,0034230607-06,0034230607-07,0034230607-08,0034230607-09,0034230607-10,0034230607-11,0034230607-12,0034230607-13,0034230607-14,0034230607-15,0034230607-16,0034230607-17,0034230607-18,0034230607-19,0034230607-20";

                List<String> subsideList = Arrays.asList(subside.split(","));
                List<String> waterPressureList = Arrays.asList(waterPressure.split(","));
                List<String> humitureList = Arrays.asList(humiture.split(","));

                // 处理subside数据
                processJinMaData(weightData.getTimestamp(), extendedStart, extendedEnd,
                        subsideList, "subside", subsideDataList);

                // 处理waterPressure数据
                processJinMaData(weightData.getTimestamp(), extendedStart, extendedEnd,
                        waterPressureList, "waterPressure", waterPressureDataList);

                // 处理humiture数据
                processJinMaData(weightData.getTimestamp(), extendedStart, extendedEnd,
                        humitureList, "humiture", humitureDataList);

//                // 对每个称重时间点查询前后6秒的高频数据
//                long queryStart = weightTime - 6;  // 前6秒
//                long queryEnd = weightTime + 6;    // 后6秒
//
//                // 确保查询时间在请求的时间范围内
//                queryStart = Math.max(queryStart, startTime);
//                queryEnd = Math.min(queryEnd, stopTime);
//
//                List<MonitorData> monitorDataList = HighSensorService.queryData(
//                        influxDBClient,
//                        fieldList,
//                        queryStart,
//                        queryEnd,
//                        samplingInterval
//                );
//
//                if (monitorDataList != null && !monitorDataList.isEmpty()) {
//                    // 写入文件，文件名包含称重时间戳
//                    Instant weightInstant = weightData.getTimestamp();
//                    ZonedDateTime zonedDateTime = weightInstant.atZone(ZoneId.of("Asia/Shanghai"));
//                    String fileName = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
//                    String filePath = applyDir + File.separator + fileName;
//
//                    downloadService.writeHighSensorDataToFile(monitorDataList, filePath);
//                    fileCount++;
//                }
            }

            // 数据写入
            downloadService.writeWeatherDataToFile(weatherDataList,applyDir + File.separator + "气象数据.xlsx");
            downloadService.writeJinMaDataToFile(subsideDataList,applyDir + File.separator + "沉降数据.xlsx","沉降数据");
            downloadService.writeJinMaDataToFile(waterPressureDataList, applyDir + File.separator + "孔隙水压力.xlsx","孔隙水压力");
            downloadService.writeJinMaDataToFile(humitureDataList,applyDir + File.separator + "温湿度数据.xlsx","温湿度数据");
            fileCount = fileCount + 4;

            // 创建完成标记文件
            File completeFlag = new File(applyDir + File.separator + "下载已完成");
            completeFlag.createNewFile();

                // 更新申请状态为"处理完成"
            DownloadApply apply = new DownloadApply()
                    .setApplyId(applyId)
                    .setStatus("下载完成")
                    .setMsg("数据已准备好，共生成" + fileCount + "个文件");
            downloadService.updateApplyStatus(influxDBClient, apply);

            System.out.println("查询下载用时： " + (System.currentTimeMillis() - start)/1000.0 + "秒");

            return CommonResult.success(null, "下载任务已开始处理");
        } catch (Exception e) {
            // 记录错误日志
            LogUtil.logOperation(userId, "DOWNLOAD3", "StartDownload3 failed: " + e.getMessage());
            // 更新申请状态为"处理失败"
            try {
                DownloadApply apply = new DownloadApply()
                        .setApplyId(applyId)
                        .setStatus("下载失败")
                        .setMsg("下载处理失败: " + e.getMessage());
                downloadService.updateApplyStatus(influxDBClient, apply);
            } catch (Exception ex) {
                LogUtil.logOperation(userId, "DOWNLOAD3", "Update status failed: " + ex.getMessage());
            }
            return CommonResult.failed("下载处理失败: " + e.getMessage());
        }
    }

    private void processJinMaData(Instant targetTime, long start, long end,
                                  List<String> fields, String type, List<JinMaData> targetList) {
        List<JinMaData> results = jinMaDataService.queryJinMaData(
                influxDBClient, start, end, fields, type);

        if (!results.isEmpty()) {
            JinMaData data = results.get(0);
            data.setTimestamp(targetTime); // 修改时间戳
            targetList.add(data);
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
