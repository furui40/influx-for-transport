package com.example.demo.service;

import com.example.demo.entity.SubsideData;
import com.example.demo.common.ItemMapping;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubsideService {

    private static String influxDbOrg;
    private static String influxDbBucket;

    @Value("${influxdb.org}")
    public void setInfluxDbOrg(String org) {
        SubsideService.influxDbOrg = org;
    }

    @Value("${influxdb.bucket}")
    public void setInfluxDbBucket(String bucket) {
        SubsideService.influxDbBucket = bucket;
    }

    public static void processFile(InfluxDBClient client, String filePath) {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        List<Point> batchPoints = new ArrayList<>(); // 用于存储批量数据

        try (FileInputStream file = new FileInputStream(new File(filePath))) {
            Workbook workbook = new XSSFWorkbook(file); // 处理 .xlsx 格式

            Sheet sheet = workbook.getSheetAt(0);

            // 获取列名行（第二行）
            Row headerRow = sheet.getRow(1);
            if (headerRow == null) {
                throw new IllegalArgumentException("文件格式错误：未找到列名行（第二行）");
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || row.getRowNum() == 1) continue; // 跳过表格名和列名行

                // 检测是否为空行
                boolean isEmptyRow = true;
                for (Cell cell : row) {
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        isEmptyRow = false;
                        break;
                    }
                }
                if (isEmptyRow) break; // 如果检测到空行，结束读取

                SubsideData subsideData = new SubsideData();
                Map<String, String> rowData = new HashMap<>();

                for (Cell cell : row) {
                    // 从列名行（第二行）获取列名
                    String columnName = ItemMapping.COLUMN_MAPPING.get(headerRow.getCell(cell.getColumnIndex()).getStringCellValue());
                    String cellValue = getCellValue(cell);

                    rowData.put(columnName, cellValue);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                // 将字符串解析为 LocalDateTime
                LocalDateTime localDateTime = LocalDateTime.parse(rowData.get("timestamp"), formatter);
                // 将 LocalDateTime 转换为 Instant
                Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

                // 创建Point对象
                Point point = Point.measurement("subside_data")
                        .time(instant, WritePrecision.NS) // 使用Instant和纳秒精度
                        .addTag("id", rowData.get("id"))
                        .addField("subside", Double.parseDouble(rowData.get("subside")));

                // 将Point转换为Line Protocol格式并加入批量列表
                batchPoints.add(point);
            }

            // 一次性写入所有数据
            if (!batchPoints.isEmpty()) {
                writeApiBlocking.writePoints(influxDbBucket, influxDbOrg, batchPoints);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    public static List<SubsideData> querySubsideData(InfluxDBClient client, Long startTime, Long stopTime, List<String> fields) {
        // 构建Flux查询语句
        StringBuilder fluxQuery = new StringBuilder();
        fluxQuery.append(String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"subside_data\") ",
                influxDbBucket, startTime.toString(), stopTime.toString()
        ));

        // 添加测点过滤条件
        if (fields != null && !fields.isEmpty()) {
            fluxQuery.append("|> filter(fn: (r) => ");
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    fluxQuery.append(" or ");
                }
                fluxQuery.append(String.format("r.id == \"%s\"", fields.get(i)));
            }
            fluxQuery.append(") ");
        }

        // 添加pivot操作
        fluxQuery.append("|> pivot(rowKey: [\"_time\"], columnKey: [\"id\"], valueColumn: \"_value\")");

        // 获取QueryApi
        QueryApi queryApi = client.getQueryApi();

        // 执行查询并映射到SubsideData对象
        List<SubsideData> subsideDataList = new ArrayList<>();
        List<FluxTable> tables = queryApi.query(fluxQuery.toString());

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                SubsideData subsideData = new SubsideData();
                subsideData.setTimestamp((Instant) record.getValueByKey("_time"));

                // 遍历所有测点字段
                Map<String, Double> subsides = new HashMap<>();
                for (String field : fields) {
                    Double value = (Double) record.getValueByKey(field);
                    if (value != null) {
                        subsides.put(field, value);
                    }
                }
                subsideData.setFieldValues(subsides);

                subsideDataList.add(subsideData);
            }
        }

        return subsideDataList;
    }
}