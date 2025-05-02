package com.example.demo.service;

import com.example.demo.entity.WeightData;
import com.example.demo.common.ItemMapping;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Service
public class DynamicWeighingService {

    private static String influxDbOrg;
    private static String influxDbBucket;

    @Value("${influxdb.org}")
    public void setInfluxDbOrg(String org) {
        DynamicWeighingService.influxDbOrg = org;
    }

    @Value("${influxdb.bucket}")
    public void setInfluxDbBucket(String bucket) {
        DynamicWeighingService.influxDbBucket = bucket;
    }

//    public static void processFile(InfluxDBClient client, String filePath) {
//        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
//        List<Point> batchPoints = new ArrayList<>(); // 用于存储批量数据
//
//        try (FileInputStream file = new FileInputStream(new File(filePath))) {
//            Workbook workbook = new XSSFWorkbook(file);
//            Sheet sheet = workbook.getSheetAt(0);
//
//            for (Row row : sheet) {
//                if (row.getRowNum() == 0) continue; // 跳过标题行
//
//                WeightData weightData = new WeightData();
//                Map<String, String> rowData = new HashMap<>();
//
//                for (Cell cell : row) {
//                    String columnName = ItemMapping.COLUMN_MAPPING.get(sheet.getRow(0).getCell(cell.getColumnIndex()).getStringCellValue());
//                    String cellValue = getCellValue(cell);
//
//
//                    rowData.put(columnName, cellValue);
//                }
//
//                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//                // 将字符串解析为 LocalDateTime
//                LocalDateTime localDateTime = LocalDateTime.parse(rowData.get("timestamp"), formatter);
//                // 将 LocalDateTime 转换为 Instant，假设使用系统默认时区
//                Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
//
//                weightData.setId(rowData.get("id").replace(".0", ""))
//                        .setTimestamp(instant)
//                        .setWeightKg(Double.parseDouble(rowData.get("weightKg")))
//                        .setVehicleLength(Double.parseDouble(rowData.get("vehicleLength")))
//                        .setLane(rowData.get("lane").replace(".0", ""))
//                        .setAxleCount(Double.valueOf(rowData.get("axleCount")))
//                        .setSpeed(Double.parseDouble(rowData.get("speed")))
//                        .setTemperature(Double.parseDouble(rowData.get("temperature")))
//                        .setDirection(rowData.get("direction"))
//                        .setCrossLane(rowData.get("crossLane"))
//                        .setType(rowData.get("type"))
//                        .setAxleWeight1(Double.parseDouble(rowData.get("axleWeight1")))
//                        .setAxleWeight2(Double.parseDouble(rowData.get("axleWeight2")))
//                        .setAxleWeight3(Double.parseDouble(rowData.get("axleWeight3")))
//                        .setAxleWeight4(Double.parseDouble(rowData.get("axleWeight4")))
//                        .setAxleWeight5(Double.parseDouble(rowData.get("axleWeight5")))
//                        .setAxleWeight6(Double.parseDouble(rowData.get("axleWeight6")))
//                        .setWheelbase1(Double.parseDouble(rowData.get("wheelbase1")))
//                        .setWheelbase2(Double.parseDouble(rowData.get("wheelbase2")))
//                        .setWheelbase3(Double.parseDouble(rowData.get("wheelbase3")))
//                        .setWheelbase4(Double.parseDouble(rowData.get("wheelbase4")))
//                        .setWheelbase5(Double.parseDouble(rowData.get("wheelbase5")))
//                        .setVehicleTypeCode(rowData.get("vehicleTypeCode"))
//                        .setVehicleType(rowData.get("vehicleType"))
//                        .setAxle1Kn(Double.parseDouble(rowData.get("axle1Kn")))
//                        .setAxle2Kn(Double.parseDouble(rowData.get("axle2Kn")))
//                        .setAxle3Kn(Double.parseDouble(rowData.get("axle3Kn")))
//                        .setOffset(Double.parseDouble(rowData.get("offset")));
//
//                // 创建Point对象
//                Point point = Point.measurement("weight_data")
//                        .time(weightData.getTimestamp(), WritePrecision.NS) // 使用Instant和纳秒精度
//                        .addField("weight_kg", weightData.getWeightKg())
//                        .addField("vehicle_length", weightData.getVehicleLength())
//                        .addField("lane", weightData.getLane())
//                        .addField("axle_count", weightData.getAxleCount())
//                        .addField("speed", weightData.getSpeed())
//                        .addField("temperature", weightData.getTemperature())
//                        .addField("direction", weightData.getDirection())
//                        .addField("cross_lane", weightData.getCrossLane())
//                        .addField("type", weightData.getType())
//                        .addField("axle_weight_1", weightData.getAxleWeight1())
//                        .addField("axle_weight_2", weightData.getAxleWeight2())
//                        .addField("axle_weight_3", weightData.getAxleWeight3())
//                        .addField("axle_weight_4", weightData.getAxleWeight4())
//                        .addField("axle_weight_5", weightData.getAxleWeight5())
//                        .addField("axle_weight_6", weightData.getAxleWeight6())
//                        .addField("wheelbase_1", weightData.getWheelbase1())
//                        .addField("wheelbase_2", weightData.getWheelbase2())
//                        .addField("wheelbase_3", weightData.getWheelbase3())
//                        .addField("wheelbase_4", weightData.getWheelbase4())
//                        .addField("wheelbase_5", weightData.getWheelbase5())
//                        .addField("vehicle_type_code", weightData.getVehicleTypeCode())
//                        .addField("vehicle_type", weightData.getVehicleType())
//                        .addField("axle_1_kn", weightData.getAxle1Kn())
//                        .addField("axle_2_kn", weightData.getAxle2Kn())
//                        .addField("axle_3_kn", weightData.getAxle3Kn())
//                        .addField("offset", weightData.getOffset())
//                        .addTag("id",weightData.getId());
//
//                // 将Point转换为Line Protocol格式并加入批量列表
//                batchPoints.add(point);
//            }
//
//            // 一次性写入所有数据
//            if (!batchPoints.isEmpty()) {
//                writeApiBlocking.writePoints(influxDbBucket, influxDbOrg, batchPoints);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void processFile(InfluxDBClient client, String filePath) {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        List<Point> batchPoints = new ArrayList<>(); // 用于存储批量数据

        try (FileInputStream file = new FileInputStream(new File(filePath))) {
            Workbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // 跳过标题行

                Map<String, String> rowData = new HashMap<>();

                for (Cell cell : row) {
                    String columnName = ItemMapping.WEIGHT_COLUMN_MAPPING.get(sheet.getRow(0).getCell(cell.getColumnIndex()).getStringCellValue());
                    String cellValue = getCellValue(cell);

                    rowData.put(columnName, cellValue);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                // 将字符串解析为 LocalDateTime
                LocalDateTime localDateTime = LocalDateTime.parse(rowData.get("timestamp"), formatter);
                // 将 LocalDateTime 转换为 Instant，假设使用系统默认时区
                Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

                // 直接创建Point对象
                Point point = Point.measurement("weight_data")
                        .time(instant, WritePrecision.NS) // 使用Instant和纳秒精度
                        .addField("weight_kg", Double.parseDouble(rowData.get("weightKg")))
                        .addField("vehicle_length", Double.parseDouble(rowData.get("vehicleLength")))
                        .addField("lane", rowData.get("lane").replace(".0", ""))
                        .addField("axle_count", Double.parseDouble(rowData.get("axleCount")))
                        .addField("speed", Double.parseDouble(rowData.get("speed")))
                        .addField("temperature", Double.parseDouble(rowData.get("temperature")))
                        .addField("direction", rowData.get("direction"))
                        .addField("cross_lane", rowData.get("crossLane"))
                        .addField("type", rowData.get("type"))
                        .addField("axle_weight_1", Double.parseDouble(rowData.get("axleWeight1")))
                        .addField("axle_weight_2", Double.parseDouble(rowData.get("axleWeight2")))
                        .addField("axle_weight_3", Double.parseDouble(rowData.get("axleWeight3")))
                        .addField("axle_weight_4", Double.parseDouble(rowData.get("axleWeight4")))
                        .addField("axle_weight_5", Double.parseDouble(rowData.get("axleWeight5")))
                        .addField("axle_weight_6", Double.parseDouble(rowData.get("axleWeight6")))
                        .addField("wheelbase_1", Double.parseDouble(rowData.get("wheelbase1")))
                        .addField("wheelbase_2", Double.parseDouble(rowData.get("wheelbase2")))
                        .addField("wheelbase_3", Double.parseDouble(rowData.get("wheelbase3")))
                        .addField("wheelbase_4", Double.parseDouble(rowData.get("wheelbase4")))
                        .addField("wheelbase_5", Double.parseDouble(rowData.get("wheelbase5")))
                        .addField("vehicle_type_code", rowData.get("vehicleTypeCode"))
                        .addField("vehicle_type", rowData.get("vehicleType"))
                        .addField("axle_1_kn", Double.parseDouble(rowData.get("axle1Kn")))
                        .addField("axle_2_kn", Double.parseDouble(rowData.get("axle2Kn")))
                        .addField("axle_3_kn", Double.parseDouble(rowData.get("axle3Kn")))
                        .addField("offset", Double.parseDouble(rowData.get("offset")))
                        .addTag("id", rowData.get("id").replace(".0", "")); // 添加标签

                // 将Point加入批量列表
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


    public static List<WeightData> queryWeightData(InfluxDBClient client, Long startTime, Long stopTime) {
        // 构建Flux查询语句
        String fluxQuery = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"weight_data\") " +
                        "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                influxDbBucket, startTime.toString(), stopTime.toString()
        );

//        System.out.println(fluxQuery);
        // 获取QueryApi
        QueryApi queryApi = client.getQueryApi();

        // 执行查询并映射到WeightData对象
        List<WeightData> weightDataList = queryApi.query(fluxQuery, WeightData.class);

        // 调整时间戳为东八区时间（即加上8小时）
//        weightDataList.forEach(weightData -> {
//            if (weightData.getTimestamp() != null) {
//                weightData.setTimestamp(weightData.getTimestamp().plus(Duration.ofHours(8)));
//            }
//        });

        return weightDataList;
    }

}