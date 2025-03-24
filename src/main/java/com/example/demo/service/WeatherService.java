package com.example.demo.service;

import com.example.demo.entity.WeatherData;
import com.example.demo.common.ItemMapping;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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

@Service
@Component
public class WeatherService {

    private static String influxDbOrg;
    private static String influxDbBucket;

    @Value("${influxdb.org}")
    public void setInfluxDbOrg(String org) {
        WeatherService.influxDbOrg = org;
    }

    @Value("${influxdb.bucket}")
    public void setInfluxDbBucket(String bucket) {
        WeatherService.influxDbBucket = bucket;
    }

//    public static void processFile(InfluxDBClient client, String filePath) {
//        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
//        List<Point> batchPoints = new ArrayList<>(); // 用于存储批量数据
//
//        try (FileInputStream file = new FileInputStream(new File(filePath))) {
//            // 判断文件扩展名，选择相应的 Workbook 类型
//            Workbook workbook;
//            if (filePath.endsWith(".xls")) {
//                workbook = new HSSFWorkbook(file); // 处理 .xls 格式
//            } else if (filePath.endsWith(".xlsx")) {
//                workbook = new XSSFWorkbook(file); // 处理 .xlsx 格式
//            } else {
//                throw new IllegalArgumentException("Unsupported file format: " + filePath);
//            }
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            for (Row row : sheet) {
//                if (row.getRowNum() == 0) continue; // 跳过标题行
//
//                WeatherData weatherData = new WeatherData();
//                Map<String, String> rowData = new HashMap<>();
//
//                for (Cell cell : row) {
//                    String columnName = ItemMapping.COLUMN_MAPPING.get(sheet.getRow(0).getCell(cell.getColumnIndex()).getStringCellValue());
//                    String cellValue = getCellValue(cell);
//
//                    rowData.put(columnName, cellValue);
//                }
//
//                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
//                // 将字符串解析为 LocalDateTime
//                LocalDateTime localDateTime = LocalDateTime.parse(rowData.get("timestamp"), formatter);
//                // 将 LocalDateTime 转换为 Instant
//                Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
//
//                // 设置 WeatherData 对象
//                weatherData.setTimestamp(instant)
//                        .setAmbientTemperature(Double.parseDouble(rowData.get("ambientTemperature")))
//                        .setTemperature1(Double.parseDouble(rowData.get("temperature1")))
//                        .setDewPointTemperature(Double.parseDouble(rowData.get("dewPointTemperature")))
//                        .setAmbientHumidity(Double.parseDouble(rowData.get("ambientHumidity")))
//                        .setAirPressure(Double.parseDouble(rowData.get("airPressure")))
//                        .setTotalRadiation1Instant(Double.parseDouble(rowData.get("totalRadiation1Instant")))
//                        .setUVRadiationInstant(Double.parseDouble(rowData.get("UVRadiationInstant")))
//                        .setWindDirection(Double.parseDouble(rowData.get("windDirection")))
//                        .setInstantWindSpeed(Double.parseDouble(rowData.get("instantWindSpeed")))
//                        .setWindSpeed2Min(Double.parseDouble(rowData.get("windSpeed2Min")))
//                        .setWindSpeed10Min(Double.parseDouble(rowData.get("windSpeed10Min")))
//                        .setRainfallIntervalAccumulated(Double.parseDouble(rowData.get("rainfallIntervalAccumulated")))
//                        .setRainfallDailyAccumulated(Double.parseDouble(rowData.get("rainfallDailyAccumulated")))
//                        .setTotalRadiation1DailyAccumulated(Double.parseDouble(rowData.get("totalRadiation1DailyAccumulated")))
//                        .setUVRadiationDailyAccumulated(Double.parseDouble(rowData.get("UVRadiationDailyAccumulated")))
//                        .setIlluminance(Double.parseDouble(rowData.get("illuminance")))
//                        .setVoltage(Double.parseDouble(rowData.get("voltage")));
//
//                // 创建Point对象
//                Point point = Point.measurement("weather_data")
//                        .time(weatherData.getTimestamp(), WritePrecision.NS) // 使用Instant和纳秒精度
//                        .addField("ambient_temperature", weatherData.getAmbientTemperature())
//                        .addField("temperature1", weatherData.getTemperature1())
//                        .addField("dew_point_temperature", weatherData.getDewPointTemperature())
//                        .addField("ambient_humidity", weatherData.getAmbientHumidity())
//                        .addField("air_pressure", weatherData.getAirPressure())
//                        .addField("total_radiation1_instant", weatherData.getTotalRadiation1Instant())
//                        .addField("uv_radiation_instant", weatherData.getUVRadiationInstant())
//                        .addField("wind_direction", weatherData.getWindDirection())
//                        .addField("instant_wind_speed", weatherData.getInstantWindSpeed())
//                        .addField("windspeed_2min", weatherData.getWindSpeed2Min())
//                        .addField("windspeed_10min", weatherData.getWindSpeed10Min())
//                        .addField("rainfall_interval_accumulated", weatherData.getRainfallIntervalAccumulated())
//                        .addField("rainfall_daily_accumulated", weatherData.getRainfallDailyAccumulated())
//                        .addField("total_radiation1_daily_accumulated", weatherData.getTotalRadiation1DailyAccumulated())
//                        .addField("uv_radiation_daily_accumulated", weatherData.getUVRadiationDailyAccumulated())
//                        .addField("illuminance", weatherData.getIlluminance())
//                        .addField("voltage", weatherData.getVoltage());
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
            // 判断文件扩展名，选择相应的 Workbook 类型
            Workbook workbook;
            if (filePath.endsWith(".xls")) {
                workbook = new HSSFWorkbook(file); // 处理 .xls 格式
            } else if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(file); // 处理 .xlsx 格式
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + filePath);
            }

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // 跳过标题行

                Map<String, String> rowData = new HashMap<>();

                for (Cell cell : row) {
                    String columnName = ItemMapping.COLUMN_MAPPING.get(sheet.getRow(0).getCell(cell.getColumnIndex()).getStringCellValue());
                    String cellValue = getCellValue(cell);

                    rowData.put(columnName, cellValue);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                // 将字符串解析为 LocalDateTime
                LocalDateTime localDateTime = LocalDateTime.parse(rowData.get("timestamp"), formatter);
                // 将 LocalDateTime 转换为 Instant
                Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

                // 直接创建Point对象
                Point point = Point.measurement("weather_data")
                        .time(instant, WritePrecision.NS) // 使用Instant和纳秒精度
                        .addField("ambient_temperature", Double.parseDouble(rowData.get("ambientTemperature")))
                        .addField("temperature1", Double.parseDouble(rowData.get("temperature1")))
                        .addField("dew_point_temperature", Double.parseDouble(rowData.get("dewPointTemperature")))
                        .addField("ambient_humidity", Double.parseDouble(rowData.get("ambientHumidity")))
                        .addField("air_pressure", Double.parseDouble(rowData.get("airPressure")))
                        .addField("total_radiation1_instant", Double.parseDouble(rowData.get("totalRadiation1Instant")))
                        .addField("uv_radiation_instant", Double.parseDouble(rowData.get("UVRadiationInstant")))
                        .addField("wind_direction", Double.parseDouble(rowData.get("windDirection")))
                        .addField("instant_wind_speed", Double.parseDouble(rowData.get("instantWindSpeed")))
                        .addField("windspeed_2min", Double.parseDouble(rowData.get("windSpeed2Min")))
                        .addField("windspeed_10min", Double.parseDouble(rowData.get("windSpeed10Min")))
                        .addField("rainfall_interval_accumulated", Double.parseDouble(rowData.get("rainfallIntervalAccumulated")))
                        .addField("rainfall_daily_accumulated", Double.parseDouble(rowData.get("rainfallDailyAccumulated")))
                        .addField("total_radiation1_daily_accumulated", Double.parseDouble(rowData.get("totalRadiation1DailyAccumulated")))
                        .addField("uv_radiation_daily_accumulated", Double.parseDouble(rowData.get("UVRadiationDailyAccumulated")))
                        .addField("illuminance", Double.parseDouble(rowData.get("illuminance")))
                        .addField("voltage", Double.parseDouble(rowData.get("voltage")));

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

    public static List<WeatherData> queryWeatherData(InfluxDBClient client, Long startTime, Long stopTime) {
        // 构建Flux查询语句
        String fluxQuery = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"weather_data\") " +
                        "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                influxDbBucket, startTime.toString(), stopTime.toString()
        );

        // 获取QueryApi
        QueryApi queryApi = client.getQueryApi();

        // 执行查询并映射到WeatherData对象
        List<WeatherData> weatherDataList = queryApi.query(fluxQuery, WeatherData.class);

        // 调整时间戳为东八区时间（即加上8小时）
//        weatherDataList.forEach(weatherData -> {
//            if (weatherData.getTimestamp() != null) {
//                weatherData.setTimestamp(weatherData.getTimestamp().plus(Duration.ofHours(8)));
//            }
//        });

        return weatherDataList;
    }
}
