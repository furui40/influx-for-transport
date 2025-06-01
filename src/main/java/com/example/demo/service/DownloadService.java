package com.example.demo.service;

import com.example.demo.common.ItemMapping;
import com.example.demo.entity.*;
import com.example.demo.common.CommonResult;
import com.example.demo.common.ResultCode;
import com.example.demo.utils.LogUtil;
import com.example.demo.utils.MailUtils;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.example.demo.common.ItemMapping.WEATHER_COLUMN_MAPPING;
import static com.example.demo.common.ItemMapping.WEIGHT_COLUMN_MAPPING;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private  final MailUtils mailUtils ;

    private static String influxDbOrg;
    private static String influxDbBucket;

    @Value("${data.download-dir:/data}")
    private String downloadDir;

    @Value("${influxdb.org}")
    public void setInfluxDbOrg(String org) {
        DownloadService.influxDbOrg = org;
    }

    @Value("${influxdb.bucket}")
    public void setInfluxDbBucket(String bucket) {
        DownloadService.influxDbBucket = bucket;
    }

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

            // 发送邮件
            mailUtils.sendApplicationEmail(apply.getUserEmail(), apply.getApplyId(), "submitted", null);

            // 返回成功响应
            return CommonResult.success(apply, "申请提交成功");
        } catch (Exception e) {
            // 返回失败响应
            return CommonResult.failed(ResultCode.FAILED, "申请提交失败: " + e.getMessage());
        }
    }

    private String buildFluxQuery(String method, String Id) {
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

        System.out.println(fluxQuery);
        return fluxQuery.toString();
    }
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

    public CommonResult<?> passApply(InfluxDBClient client, String applyId) {
        if (client == null || applyId == null) {
            return CommonResult.validateFailed("参数不能为空");
        }

        try {
            findAndUpdateApply(client, applyId, "审核通过", "审核通过", "approved", null);
            return CommonResult.success("更新成功");
        } catch (Exception e) {
            LogUtil.logOperation(applyId, "PASS", "PassApply failed: " + e.getMessage());
            return CommonResult.failed("审核通过失败: " + e.getMessage());
        }
    }

    public CommonResult<?> rejectApply(InfluxDBClient client, String applyId, String reason) {
        if (client == null || applyId == null) {
            return CommonResult.validateFailed("参数不能为空");
        }

        try {
            findAndUpdateApply(client, applyId, "审核不通过", reason, "rejected", reason);
            return CommonResult.success("更新成功");
        } catch (Exception e) {
            LogUtil.logOperation(applyId, "REJECT", "RejectApply failed: " + e.getMessage());
            return CommonResult.failed("审核拒绝失败: " + e.getMessage());
        }
    }

    public void writeHighSensorDataToFile(List<MonitorData> monitorDataList, String filePath) {
        if (monitorDataList == null || monitorDataList.isEmpty()) {
            return;
        }

        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("高频传感器数据");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("时间戳(东八区)");

            List<String> fields = monitorDataList.get(0).getFields();
            for (int i = 0; i < fields.size(); i++) {
                headerRow.createCell(i + 1).setCellValue(fields.get(i));
            }

            // 填充数据
            for (int i = 0; i < monitorDataList.size(); i++) {
                MonitorData data = monitorDataList.get(i);
                Row row = sheet.createRow(i + 1);

                // 写入时间戳（转换为东八区）
                Instant instant = data.getTime();
                String localTime = timeFormatter.format(instant);
                row.createCell(0).setCellValue(localTime);

                // 按字段顺序写入值
                List<Double> values = data.getValues();
                for (int j = 0; j < values.size(); j++) {
                    Double value = values.get(j);
                    if (value != null && !value.isNaN()) {
                        row.createCell(j + 1).setCellValue(value);
                    } else {
                        row.createCell(j + 1).setCellValue(""); // 空值处理
                    }
                }
            }

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("写入文件失败: " + e.getMessage(), e);
        }
    }


    public void writeWeightDataToFile(List<WeightData> weightDataList, String filePath) {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("动态称重数据");

            // 创建表头（直接使用 WEIGHT_COLUMN_MAPPING 的键）
            Row headerRow = sheet.createRow(0);
            int colNum = 0;
            for (String columnName : WEIGHT_COLUMN_MAPPING.keySet()) {
                headerRow.createCell(colNum++).setCellValue(columnName);
            }

            // 填充数据
            int rowNum = 1;
            for (WeightData data : weightDataList) {
                Row row = sheet.createRow(rowNum++);
                colNum = 0;

                for (String fieldName : WEIGHT_COLUMN_MAPPING.values()) {
                    Object value = getFieldValue(data, fieldName, timeFormatter);
                    if (value != null) {
                        row.createCell(colNum).setCellValue(value.toString());
                    }
                    colNum++;
                }
            }

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        } catch (IOException | ReflectiveOperationException e) {
            throw new RuntimeException("写入称重数据文件失败", e);
        }
    }

    public void writeWeatherDataToFile(List<WeatherData> weatherDataList, String filePath) {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("气象数据");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            int colNum = 0;
            for (String columnName : WEATHER_COLUMN_MAPPING.keySet()) {
                headerRow.createCell(colNum++).setCellValue(columnName);
            }

            // 填充数据
            int rowNum = 1;
            for (WeatherData data : weatherDataList) {
                Row row = sheet.createRow(rowNum++);
                colNum = 0;

                for (String fieldName : WEATHER_COLUMN_MAPPING.values()) {
                    Object value = getFieldValue(data, fieldName, timeFormatter);
                    if (value != null) {
                        row.createCell(colNum).setCellValue(value.toString());
                    }
                    colNum++;
                }
            }

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        } catch (IOException | ReflectiveOperationException e) {
            throw new RuntimeException("写入气象数据文件失败", e);
        }
    }

    public void writeJinMaDataToFile(List<JinMaData> jinMaDataList, String filePath, String dataType) {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(dataType);

            // 获取所有字段名（排除timestamp），并按字典序排序
            Set<String> fieldNames = jinMaDataList.stream()
                    .flatMap(data -> data.getFieldValues().keySet().stream())
                    .collect(Collectors.toCollection(TreeSet::new)); // TreeSet自动排序

            // 创建表头（时间戳 + 排序后的字段名）
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("时间戳");
            int colNum = 1;
            for (String fieldName : fieldNames) {
                headerRow.createCell(colNum++).setCellValue(fieldName);
            }

            // 填充数据
            int rowNum = 1;
            for (JinMaData data : jinMaDataList) {
                Row row = sheet.createRow(rowNum++);

                // 时间戳（第0列）
                row.createCell(0).setCellValue(timeFormatter.format(data.getTimestamp()));

                // 动态字段（从第1列开始）
                colNum = 1;
                for (String fieldName : fieldNames) {
                    Double value = data.getFieldValues().get(fieldName);
                    if (value != null) {
                        row.createCell(colNum).setCellValue(value);
                    }
                    colNum++;
                }
            }

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("写入金马数据文件失败", e);
        }
    }

    // 通用方法：通过反射获取字段值
    private Object getFieldValue(WeightData data, String fieldName, DateTimeFormatter timeFormatter)
            throws ReflectiveOperationException {
        if ("timestamp".equals(fieldName)) {
            return timeFormatter.format(data.getTimestamp());
        }

        // 通过反射获取其他字段值
        String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method getter = WeightData.class.getMethod(getterName);
        return getter.invoke(data);
    }

    private Object getFieldValue(WeatherData data, String fieldName, DateTimeFormatter timeFormatter)
            throws ReflectiveOperationException {
        if ("timestamp".equals(fieldName)) {
            return timeFormatter.format(data.getTimestamp());
        }

        // 通过反射获取其他字段值
        String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method getter = WeatherData.class.getMethod(getterName);
        return getter.invoke(data);
    }

    public void findAndUpdateApply(
            InfluxDBClient client,
            String applyId,
            String status,
            String message,
            String emailType,
            String reason) {
        try {
            // 构建查询
            String fluxQuery = buildFluxQuery("2", applyId);
            QueryApi queryApi = client.getQueryApi();

            // 执行查询
            List<DownloadApply> downloadApplyList = queryApi.query(fluxQuery, DownloadApply.class);
            if (downloadApplyList.size() == 1) {
                DownloadApply apply = downloadApplyList.get(0);

                // 创建更新点
                Point point = Point.measurement("apply_data")
                        .addTag("apply_id", apply.getApplyId())
                        .addField("status", status)
                        .addTag("user_id", apply.getUserId())
                        .addField("data_type", apply.getDataType())
                        .addField("fields", apply.getFields())
                        .addField("start_time", apply.getStartTime().toString())
                        .addField("stop_time", apply.getStopTime().toString())
                        .addField("user_email", apply.getUserEmail())
                        .addField("msg", message)
                        .time(apply.getTimestamp(), WritePrecision.NS);

                // 写入更新
                client.getWriteApi().writePoint(influxDbBucket, influxDbOrg, point);

                // 发送邮件通知
                if (emailType != null) {
                    mailUtils.sendApplicationEmail(
                            apply.getUserEmail(),
                            apply.getApplyId(),
                            emailType,
                            reason
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("更新申请状态失败: " + e.getMessage(), e);
        }
    }

    public CommonResult<?> sendDownloadLink(InfluxDBClient client, String applyId, String downloadLink) {
        try {
            // 1. 查询申请记录获取用户邮箱
            String fluxQuery = buildFluxQuery("2", applyId);
            QueryApi queryApi = client.getQueryApi();
            List<DownloadApply> applyList = queryApi.query(fluxQuery, DownloadApply.class);

            if (applyList.isEmpty()) {
                return CommonResult.failed("未找到对应的申请记录");
            }

            DownloadApply apply = applyList.get(0);

            // 2. 发送邮件
            mailUtils.sendDownloadLinkEmail(apply.getUserEmail(), applyId, downloadLink);

            // 3. 使用共用方法更新申请状态
            findAndUpdateApply(
                    client,
                    applyId,
                    "已完成",
                    "下载链接已发送",
                    null,  // 不需要发送状态变更邮件
                    null   // 不需要原因
            );

            return CommonResult.success("邮件发送成功");
        } catch (Exception e) {
            LogUtil.logOperation(applyId, "SEND_LINK", "发送下载链接失败: " + e.getMessage());
            return CommonResult.failed("邮件发送失败: " + e.getMessage());
        }
    }

    public void updateApplyStatus(InfluxDBClient client, DownloadApply apply) {
        findAndUpdateApply(
                client,
                apply.getApplyId(),
                apply.getStatus(),
                apply.getMsg(),
                null,
                null
        );
    }
}