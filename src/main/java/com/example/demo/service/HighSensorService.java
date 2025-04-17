package com.example.demo.service;

import com.example.demo.entity.MonitorData;
import com.example.demo.utils.*;
import com.influxdb.client.InfluxDBClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class HighSensorService {

    @Value("${data.base-dir:/data}")
    private String baseDir;

    private static final String STATUS_FILE = "highsensor_status.txt";
    private static final String REVISE_STATUS_FILE = "highsensor_revise_status.txt";
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 初始化服务，创建必要的状态文件
     */
    public void initialize() throws IOException {
        Path statusPath = Paths.get(baseDir, STATUS_FILE);
        Path reviseStatusPath = Paths.get(baseDir, REVISE_STATUS_FILE);

        if (!Files.exists(statusPath)) {
            Files.createFile(statusPath);
        }

        if (!Files.exists(reviseStatusPath)) {
            Files.createFile(reviseStatusPath);
        }
    }

    /**
     * 处理单个数据文件
     * @param client InfluxDB客户端
     * @param filePath 文件路径
     */
    public void processDataFile(InfluxDBClient client, String filePath) throws IOException {
        // 检查文件是否已处理
        if (isFileProcessed(filePath)) {
            return;
        }

        // 写入数据到数据库
        DBUtilInsert.writeDataFromFile1(client, filePath);

        // 更新状态文件
        updateStatusFile(filePath, true);

        // 检查是否需要执行数据修正
        checkAndPerformDataRevise(client);
    }

    /**
     * 查询数据
     * @param client InfluxDB客户端
     * @param fields 需要查询的字段列表
     * @param startTime 开始时间(秒)
     * @param stopTime 结束时间(秒)
     * @param samplingInterval 采样间隔
     * @return 查询结果列表
     */
    public List<MonitorData> queryData(InfluxDBClient client, List<String> fields,
                                       Long startTime, Long stopTime, Long samplingInterval) {
        return DBUtilSearch.BaseQuery(client, fields, startTime, stopTime, samplingInterval);
    }

    /**
     * 检查文件是否已处理
     * @param filePath 文件路径
     * @return 是否已处理
     */
    private boolean isFileProcessed(String filePath) throws IOException {
        Path statusPath = Paths.get(baseDir, STATUS_FILE);
        if (!Files.exists(statusPath)) {
            return false;
        }

        try (BufferedReader reader = Files.newBufferedReader(statusPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 1 && parts[0].equals(filePath) &&
                        parts.length >= 2 && parts[1].equals("已写入")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 更新状态文件
     * @param filePath 文件路径
     * @param processed 是否已处理
     */
    private void updateStatusFile(String filePath, boolean processed) throws IOException {
        Path statusPath = Paths.get(baseDir, STATUS_FILE);
        List<String> lines = new ArrayList<>();

        // 读取现有内容
        if (Files.exists(statusPath)) {
            lines = Files.readAllLines(statusPath);
        }

        // 更新或添加记录
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String[] parts = lines.get(i).split("\t");
            if (parts.length > 0 && parts[0].equals(filePath)) {
                lines.set(i, filePath + "\t" + (processed ? "已写入" : "未写入"));
                found = true;
                break;
            }
        }

        if (!found) {
            lines.add(filePath + "\t" + (processed ? "已写入" : "未写入"));
        }

        // 写入文件
        Files.write(statusPath, lines);
    }

    /**
     * 检查并执行数据修正
     * @param client InfluxDB客户端
     */
    private void checkAndPerformDataRevise(InfluxDBClient client) throws IOException {
        // 获取所有已写入但未修正的文件
        Map<String, Set<String>> groupFiles = getGroupedFilesByTime();

        // 检查每组数据是否完整
        for (Map.Entry<String, Set<String>> entry : groupFiles.entrySet()) {
            String timeKey = entry.getKey();
            Set<String> files = entry.getValue();

            // 检查第一组(decoder1和decoder2)
            if (files.contains("01") && files.contains("02")) {
                if (!isReviseCompleted(timeKey, 1)) {
                    performReviseForGroup(client, timeKey, 1);
                    updateReviseStatus(timeKey, 1);
                }
            }

            // 检查第二组(decoder3和decoder4)
            if (files.contains("03") && files.contains("04")) {
                if (!isReviseCompleted(timeKey, 2)) {
                    performReviseForGroup(client, timeKey, 2);
                    updateReviseStatus(timeKey, 2);
                }
            }
        }
    }

    /**
     * 按时间分组获取已写入的文件
     * @return 按时间分组的文件映射
     */
    private Map<String, Set<String>> getGroupedFilesByTime() throws IOException {
        Path statusPath = Paths.get(baseDir, STATUS_FILE);
        Map<String, Set<String>> result = new HashMap<>();

        if (!Files.exists(statusPath)) {
            return result;
        }

        try (BufferedReader reader = Files.newBufferedReader(statusPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 2 && parts[1].equals("已写入")) {
                    String filePath = parts[0];
                    String fileName = Paths.get(filePath).getFileName().toString();

                    // 解析文件名获取时间和decoder编号
                    if (fileName.startsWith("Wave_") && fileName.endsWith(".txt")) {
                        String timePart = fileName.substring(5, fileName.length() - 4);
                        String decoderId = Paths.get(filePath).getParent().getFileName().toString();

                        // 确保decoderId是两位数
                        if (decoderId.length() == 1) {
                            decoderId = "0" + decoderId;
                        }

                        result.computeIfAbsent(timePart, k -> new HashSet<>()).add(decoderId);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 检查修正是否已完成
     * @param timeKey 时间键
     * @param group 组号(1或2)
     * @return 是否已完成
     */
    private boolean isReviseCompleted(String timeKey, int group) throws IOException {
        Path reviseStatusPath = Paths.get(baseDir, REVISE_STATUS_FILE);
        if (!Files.exists(reviseStatusPath)) {
            return false;
        }

        try (BufferedReader reader = Files.newBufferedReader(reviseStatusPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 2 && parts[0].equals(timeKey) &&
                        parts[1].equals(String.valueOf(group))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 执行数据修正
     * @param client InfluxDB客户端
     * @param timeKey 时间键
     * @param group 组号(1或2)
     */
    private void performReviseForGroup(InfluxDBClient client, String timeKey, int group) throws IOException {
        // 解析时间范围
        LocalDateTime fileTime = LocalDateTime.parse(timeKey, FILE_TIME_FORMATTER);
        Instant startTime = Instant.from(fileTime);
        Instant endTime = startTime.plusSeconds(3600); // 假设每个文件包含1小时的数据

        // 执行修正
        DataRevise.dataRevise(client, startTime, endTime, group);
    }

    /**
     * 更新修正状态
     * @param timeKey 时间键
     * @param group 组号(1或2)
     */
    private void updateReviseStatus(String timeKey, int group) throws IOException {
        Path reviseStatusPath = Paths.get(baseDir, REVISE_STATUS_FILE);
        List<String> lines = new ArrayList<>();

        // 读取现有内容
        if (Files.exists(reviseStatusPath)) {
            lines = Files.readAllLines(reviseStatusPath);
        }

        // 添加新记录
        lines.add(timeKey + "\t" + group + "\t" + Instant.now().toString());

        // 写入文件
        Files.write(reviseStatusPath, lines);
    }
}