package com.example.demo.service;

import com.example.demo.entity.DownloadApply;
import com.example.demo.common.CommonResult;
import com.example.demo.common.ResultCode;
import com.example.demo.entity.WeatherData;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DownloadService {

    private static String influxDbOrg;
    private static String influxDbBucket;

    @Value("${influxdb.org}")
    public void setInfluxDbOrg(String org) {
        DownloadService.influxDbOrg = org;
    }

    @Value("${influxdb.bucket}")
    public void setInfluxDbBucket(String bucket) {
        DownloadService.influxDbBucket = bucket;
    }

    /**
     * 将 DownloadApply 对象以 Point 数据类型写入 InfluxDB
     *
     * @param client InfluxDB 客户端
     * @param apply  DownloadApply 对象
     * @return CommonResult 表示操作结果
     */
    public CommonResult<DownloadApply> addNewApply(InfluxDBClient client, DownloadApply apply) {
        if (client == null || apply == null) {
            return CommonResult.validateFailed("参数不能为空");
        }

        try {
            // 创建 Point 对象
            Point point = Point.measurement("apply_data")
                    .addTag("apply_id", apply.getApplyId())
                    .addField("status", apply.getStatus())
                    .addTag("user_id", apply.getUserId())
                    .addField("data_type", apply.getDataType())
                    .addField("fields", apply.getFields())
                    .addField("start_time", apply.getStartTime().toString())
                    .addField("stop_time", apply.getStopTime().toString())
                    .addField("user_email", apply.getUserEmail())
                    .addField("msg","已申请")
                    .time(Instant.now(), WritePrecision.NS); // 使用 applyId 作为时间戳

            // 获取 WriteApi
            WriteApi writeApi = client.getWriteApi();

            // 写入 InfluxDB
            writeApi.writePoint(influxDbBucket, influxDbOrg, point);

            // 返回成功响应
            return CommonResult.success(apply, "申请提交成功");
        } catch (Exception e) {
            // 返回失败响应
            return CommonResult.failed(ResultCode.FAILED, "申请提交失败: " + e.getMessage());
        }
    }

    /**
     * 查询历史申请数据
     *
     * @param client InfluxDB 客户端
     * @param method 查询方法，"1" 表示需要过滤 userId，"2" 表示不需要
     * @param userId 用户 ID
     * @return CommonResult 包含查询结果
     */
    public CommonResult<?> searchApply(InfluxDBClient client, String method, String userId) {
        if (client == null || method == null || userId == null) {
            return CommonResult.validateFailed("参数不能为空");
        }

        try {
            // 构建 Flux 查询语句
            String fluxQuery = buildFluxQuery(method, userId);

            // 获取 QueryApi
            QueryApi queryApi = client.getQueryApi();

            // 执行查询
            List<DownloadApply> downloadApplyList = queryApi.query(fluxQuery,DownloadApply.class);
//            for(DownloadApply downloadApply : downloadApplyList){
//                System.out.println(downloadApply);
//            }
            // 返回查询结果
            return CommonResult.success(downloadApplyList, "查询成功");
        } catch (Exception e) {
            // 返回失败响应
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }


    /**
     * 构建 Flux 查询语句
     *
     * @param method 查询方法，"1" 表示需要过滤 userId，"2" 表示不需要
     * @param Id  ID
     * @return Flux 查询语句
     */
    private static String buildFluxQuery(String method, String Id) {
        StringBuilder fluxQuery = new StringBuilder();
        fluxQuery.append("from(bucket: \"" + influxDbBucket + "\") ")
                .append("|> range(start: 0) ")
                .append("|> filter(fn: (r) => r._measurement == \"apply_data\") ");

        // 根据 method 决定是否过滤 userId
        if ("1".equals(method)) {
            fluxQuery.append("|> filter(fn: (r) => r.user_id == \"").append(Id).append("\") ");
        }
        if ("2".equals(method)) {
            fluxQuery.append("|> filter(fn: (r) => r.apply_id == \"").append(Id).append("\") ");
        }

        fluxQuery.append("|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")");

        return fluxQuery.toString();
    }

    public static CommonResult<?> passApply(InfluxDBClient client, String applyId) {
        if (client == null || applyId == null) {
            return CommonResult.validateFailed("参数不能为空");
        }

        try {
            // 构建 Flux 查询语句
            String fluxQuery = buildFluxQuery("2", applyId);

            // 获取 QueryApi
            QueryApi queryApi = client.getQueryApi();

            // 执行查询
            List<DownloadApply> downloadApplyList = queryApi.query(fluxQuery,DownloadApply.class);
            if(downloadApplyList.size() == 1){
                for(DownloadApply apply : downloadApplyList){
                    System.out.println(apply);
                    Point point = Point.measurement("apply_data")
                            .addTag("apply_id", apply.getApplyId())
                            .addField("status", "审核通过")
                            .addTag("user_id", apply.getUserId())
                            .addField("data_type", apply.getDataType())
                            .addField("fields", apply.getFields())
                            .addField("start_time", apply.getStartTime().toString())
                            .addField("stop_time", apply.getStopTime().toString())
                            .addField("user_email", apply.getUserEmail())
                            .addField("msg","审核通过")
                            .time(apply.getTimestamp(), WritePrecision.NS);
                    WriteApi writeApi = client.getWriteApi();
                    writeApi.writePoint(influxDbBucket, influxDbOrg, point);
                }
            }
            // 返回查询结果
            return CommonResult.success("更新成功");
        } catch (Exception e) {
            // 返回失败响应
            return CommonResult.failed("更新失败: " + e.getMessage());
        }
    }

    public static CommonResult<?> rejectApply(InfluxDBClient client, String applyId, String reason) {
        if (client == null || applyId == null) {
            return CommonResult.validateFailed("参数不能为空");
        }

        try {
            // 构建 Flux 查询语句
            String fluxQuery = buildFluxQuery("2", applyId);

            // 获取 QueryApi
            QueryApi queryApi = client.getQueryApi();

            // 执行查询
            List<DownloadApply> downloadApplyList = queryApi.query(fluxQuery,DownloadApply.class);
            if(downloadApplyList.size() == 1){
                for(DownloadApply apply : downloadApplyList){
                    System.out.println(apply);
                    Point point = Point.measurement("apply_data")
                            .addTag("apply_id", apply.getApplyId())
                            .addField("status", "审核不通过")
                            .addTag("user_id", apply.getUserId())
                            .addField("data_type", apply.getDataType())
                            .addField("fields", apply.getFields())
                            .addField("start_time", apply.getStartTime().toString())
                            .addField("stop_time", apply.getStopTime().toString())
                            .addField("user_email", apply.getUserEmail())
                            .addField("msg",reason)
                            .time(apply.getTimestamp(), WritePrecision.NS);
                    WriteApi writeApi = client.getWriteApi();
                    writeApi.writePoint(influxDbBucket, influxDbOrg, point);
                }
            }
            // 返回查询结果
            return CommonResult.success("更新成功");
        } catch (Exception e) {
            // 返回失败响应
            return CommonResult.failed("更新失败: " + e.getMessage());
        }
    }
}