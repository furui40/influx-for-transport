package com.example.demo.service;

import com.example.demo.entity.JinMaData;
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
public class JinMaDataService {

    private static String influxDbOrg;
    private static String influxDbBucket;

    @Value("${influxdb.org}")
    public void setInfluxDbOrg(String org) {
        JinMaDataService.influxDbOrg = org;
    }

    @Value("${influxdb.bucket}")
    public void setInfluxDbBucket(String bucket) {
        JinMaDataService.influxDbBucket = bucket;
    }

    public static void processFile(InfluxDBClient client, String filePath, String dataType) {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        List<Point> batchPoints = new ArrayList<>();

        try (FileInputStream file = new FileInputStream(new File(filePath))) {
            Workbook workbook;
            if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(file);
            } else if (filePath.endsWith(".xls")) {
                workbook = new HSSFWorkbook(file);
            } else {
                throw new IllegalArgumentException("Unsupported file format");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(1);
            if (headerRow == null) {
                throw new IllegalArgumentException("文件格式错误：未找到列名行（第二行）");
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || row.getRowNum() == 1) continue;

                boolean isEmptyRow = true;
                for (Cell cell : row) {
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        isEmptyRow = false;
                        break;
                    }
                }
                if (isEmptyRow) break;

                Map<String, String> rowData = new HashMap<>();
                for (Cell cell : row) {
                    String headerCellValue = headerRow.getCell(cell.getColumnIndex()).getStringCellValue();
                    String columnName = ItemMapping.COLUMN_MAPPING.get(headerCellValue);
                    if (columnName == null) continue;
                    String cellValue = getCellValue(cell);
                    rowData.put(columnName, cellValue);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                LocalDateTime localDateTime = LocalDateTime.parse(rowData.get("timestamp"), formatter);
                Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

                Point point = createPointByDataType(dataType, instant, rowData);
                if (point != null) {
                    batchPoints.add(point);
                }
            }

            if (!batchPoints.isEmpty()) {
                writeApiBlocking.writePoints(influxDbBucket, influxDbOrg, batchPoints);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Point createPointByDataType(String dataType, Instant instant, Map<String, String> rowData) {
        switch (dataType) {
            case "subside":
                return Point.measurement("subside_data")
                        .time(instant, WritePrecision.NS)
                        .addTag("id", rowData.get("id"))
                        .addField("subside", Double.parseDouble(rowData.get("subside")));
            case "waterPressure":
                return Point.measurement("waterPressure_data")
                        .time(instant, WritePrecision.NS)
                        .addTag("id", rowData.get("id"))
                        .addField("waterPressure", Double.parseDouble(rowData.get("waterPressure")));
            case "humiture":
                return Point.measurement("humiture_data")
                        .time(instant, WritePrecision.NS)
                        .addTag("id", rowData.get("id"))
                        .addField("temperature", Double.parseDouble(rowData.get("temperature")))
                        .addField("wet", Double.parseDouble(rowData.get("wet")));
            default:
                throw new IllegalArgumentException("Unsupported data type: " + dataType);
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

    public static List<JinMaData> queryJinMaData(InfluxDBClient client, Long startTime, Long stopTime, List<String> fields, String dataType) {
        String measurementName = getMeasurementName(dataType);
        StringBuilder fluxQuery = new StringBuilder();
        fluxQuery.append(String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"%s\") ",
                influxDbBucket, startTime.toString(), stopTime.toString(), measurementName
        ));

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
        if(dataType.equals("humiture")){
            fluxQuery.append("|> pivot(rowKey: [\"_time\"], columnKey: [\"id\", \"_field\"], valueColumn: \"_value\")");
        }else{
            fluxQuery.append("|> pivot(rowKey: [\"_time\"], columnKey: [\"id\"], valueColumn: \"_value\")");
        }


//        System.out.println("fluxQuery: " + fluxQuery + "dataType: " + dataType);

        QueryApi queryApi = client.getQueryApi();
        List<JinMaData> jinMaDataList = new ArrayList<>();
        List<FluxTable> tables = queryApi.query(fluxQuery.toString());

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                JinMaData jinMaData = new JinMaData();
                jinMaData.setTimestamp((Instant) record.getValueByKey("_time"));

                Map<String, Double> fieldValues = new HashMap<>();
                if ("humiture".equals(dataType)) {
                    // 温湿度数据处理
                    for (String field : fields) {
                        // 温度字段
                        String tempField = field + "_temperature";
                        Double tempValue = (Double) record.getValueByKey(tempField);
                        if (tempValue != null) {
                            fieldValues.put(field + "_t", tempValue);
                        }

                        // 湿度字段
                        String wetField = field + "_wet";
                        Double wetValue = (Double) record.getValueByKey(wetField);
                        if (wetValue != null) {
                            fieldValues.put(field + "_w", wetValue);
                        }
                    }
                } else {
                    // 其他数据类型处理
                    for (String field : fields) {
                        Double value = (Double) record.getValueByKey(field);
                        if (value != null) {
                            fieldValues.put(field, value);
                        }
                    }
                }
                jinMaData.setFieldValues(fieldValues);
                jinMaDataList.add(jinMaData);
            }
        }

        return jinMaDataList;
    }

    private static String getMeasurementName(String dataType) {
        switch (dataType) {
            case "subside":
                return "subside_data";
            case "waterPressure":
                return "waterPressure_data";
            case "humiture":
                return "humiture_data";
            default:
                throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }
    }
}