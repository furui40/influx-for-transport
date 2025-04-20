package com.example.demo.service;

import com.example.demo.entity.FileStatus;
import com.example.demo.entity.MonitorData;
import com.example.demo.utils.DBUtilSearch;
import com.example.demo.utils.DataRevise;
import com.example.demo.utils.DBUtilInsert;
import com.example.demo.utils.LogUtil;
import com.influxdb.client.InfluxDBClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HighSensorService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String HIGH_SENSOR_REVISE_STATUS = "highSensor_revise_status.txt";

    public static List<MonitorData> queryData(InfluxDBClient client, List<String> fields,
                                              Long startTime, Long stopTime, Long samplingInterval) {
        return DBUtilSearch.BaseQuery(client, fields, startTime, stopTime, samplingInterval);
    }

    public static void processFile(InfluxDBClient influxDBClient, String filePath) throws IOException {
        DBUtilInsert.writeDataFromFile1(influxDBClient, filePath);
    }

    public static void checkAndPerformDataRevise(InfluxDBClient client, String baseDir, String filePath) throws IOException {
        // 1. 首先在修正状态文件中添加当前文件记录（如果不存在）
        addFileToReviseStatusIfNotExists(baseDir, filePath);

        // 2. 检查是否需要修正
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        String decoderId = path.getParent().getFileName().toString();

        if (!fileName.startsWith("Wave_") || !fileName.endsWith(".txt")) {
            return;
        }

        // 解析时间部分
        String timePart = fileName.substring(5, fileName.length() - 4);

        // 确保decoderId是两位数
        if (decoderId.length() == 1) {
            decoderId = "0" + decoderId;
        }

        // 检查是否属于需要修正的组
        int group = getGroupForDecoder(decoderId);
        if (group == 0) {
            return;
        }

        // 获取同组所有文件
        List<FileStatus> groupFiles = getGroupFiles(baseDir, timePart, group);

        // 检查是否所有文件都已写入但未修正
        boolean noneRevised = groupFiles.size() == 2 && groupFiles.stream().noneMatch(fs -> {
            try {
                return isFileRevised(baseDir, fs.getFilePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        if (noneRevised) {
            // 执行修正
            performReviseForGroup(client, timePart, group);

            // 更新修正状态（标记为已修正）
            markFilesAsRevised(baseDir, groupFiles);
        }
    }

    private static void addFileToReviseStatusIfNotExists(String baseDir, String filePath) throws IOException {
        Path reviseStatusFile = Paths.get(baseDir, HIGH_SENSOR_REVISE_STATUS);
        List<FileStatus> reviseStatus = new ArrayList<>();

        // 读取现有修正状态
        if (Files.exists(reviseStatusFile)) {
            reviseStatus = loadStatusFile(reviseStatusFile);
        }

        // 检查是否已存在该文件记录
        boolean exists = reviseStatus.stream()
                .anyMatch(fs -> fs.getFilePath().equals(filePath));

        // 如果不存在则添加新记录（状态为未修正）
        if (!exists) {
            reviseStatus.add(new FileStatus(filePath, false));
            saveStatusFile(reviseStatusFile, reviseStatus);
        }
    }

    private static void markFilesAsRevised(String baseDir, List<FileStatus> files) throws IOException {
        Path reviseStatusFile = Paths.get(baseDir, HIGH_SENSOR_REVISE_STATUS);
        List<FileStatus> reviseStatus = loadStatusFile(reviseStatusFile);

        // 更新指定文件的状态
        for (FileStatus file : files) {
            for (int i = 0; i < reviseStatus.size(); i++) {
                if (reviseStatus.get(i).getFilePath().equals(file.getFilePath())) {
                    reviseStatus.set(i, new FileStatus(file.getFilePath(), true));
                    break;
                }
            }
        }

        saveStatusFile(reviseStatusFile, reviseStatus);
    }

    private static int getGroupForDecoder(String decoderId) {
        if (decoderId.equals("01") || decoderId.equals("02")) {
            return 1;
        } else if (decoderId.equals("03") || decoderId.equals("04")) {
            return 2;
        }
        return 0;
    }

    private static List<FileStatus> getGroupFiles(String baseDir, String timeKey, int group) throws IOException {
        Path statusFile = Paths.get(baseDir, "highSensor_revise_status.txt");
        List<FileStatus> allFiles = loadStatusFile(statusFile);

        // 确定需要检查的decoder ID
        String[] targetDecoders = group == 1 ? new String[]{"01", "02"} : new String[]{"03", "04"};

        return allFiles.stream()
                .filter(fs -> {
                    Path path = Paths.get(fs.getFilePath());
                    String fileName = path.getFileName().toString();
                    String decoderId = path.getParent().getFileName().toString();

                    if (decoderId.length() == 1) {
                        decoderId = "0" + decoderId;
                    }

                    return fileName.startsWith("Wave_" + timeKey) &&
                            Arrays.asList(targetDecoders).contains(decoderId);
                })
                .collect(Collectors.toList());
    }

    private static boolean isFileRevised(String baseDir, String filePath) throws IOException {
        Path reviseStatusFile = Paths.get(baseDir, HIGH_SENSOR_REVISE_STATUS);
        if (!Files.exists(reviseStatusFile)) {
            return false;
        }

        List<FileStatus> reviseStatus = loadStatusFile(reviseStatusFile);
        return reviseStatus.stream()
                .anyMatch(fs -> fs.getFilePath().equals(filePath) && fs.isProcessed());
    }

    private static void performReviseForGroup(InfluxDBClient client, String timeKey, int group) {
        // 解析时间范围（假设每个文件包含1小时数据）
        LocalDateTime fileTime = LocalDateTime.parse(timeKey, FILE_TIME_FORMATTER);
        Instant startTime = fileTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant();
        Instant endTime = startTime.plusSeconds(3600); // 1小时 = 3600秒

        // 获取同组的解调器ID列表
        List<String> decoderIds = getDecodersInGroup(group);
        if (decoderIds.isEmpty()) {
            return;
        }

        // 最后一秒的时间范围
//        Instant lastSecondStart = endTime.minusSeconds(1);
//        Instant lastSecondEnd = endTime;
        Instant lastSecondStart = startTime;
        Instant lastSecondEnd = startTime.plusSeconds(1);

        boolean allDataPresent;
        int retryCount = 0;
        final int maxRetries = 30; // 最大重试30次
        final long retryInterval = 1000; // 每次等待1秒

        do {
            allDataPresent = true;
            for (String decoderId : decoderIds) {
                // 转换为字段名（例如 "01" -> 1_Ch1_ori）
                int decoderNum = Integer.parseInt(decoderId);
                String field = String.format("%d_Ch1_ori", decoderNum);

                // 查询最后一秒数据
                List<MonitorData> data = HighSensorService.queryData(
                        client,
                        Collections.singletonList(field),
                        lastSecondStart.getEpochSecond(),
                        lastSecondEnd.getEpochSecond(),
                        100L // samplingInterval
                );

                if (data.isEmpty()) {
                    allDataPresent = false;
                    // 记录等待日志
                    String details = String.format("等待解调器 %s 数据，时间: %s", decoderId, lastSecondStart);
                    LogUtil.logOperation("001", "DATA_REVISE_WAIT", details);
                    break;
                }
            }

            if (!allDataPresent) {
                retryCount++;
                if (retryCount > maxRetries) {
                    throw new RuntimeException("等待超时，无法获取组 " + group + " 的数据");
                }
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("修正等待被中断", e);
                }
            }
        } while (!allDataPresent);

        // 执行修正
        try {
            DataRevise.dataRevise(client, startTime, endTime, group);
            LogUtil.logOperation("001", "DATA_REVISE", "修正组 " + group + " 数据完成");
        } catch (IOException e) {
            throw new RuntimeException("修正数据失败", e);
        }
    }

    private static List<String> getDecodersInGroup(int group) {
        if (group == 1) {
            return Arrays.asList("01", "02");
        } else if (group == 2) {
            return Arrays.asList("03", "04");
        }
        return Collections.emptyList();
    }

    private static List<FileStatus> loadStatusFile(Path statusFile) throws IOException {
        List<FileStatus> result = new ArrayList<>();

        if (!Files.exists(statusFile)) {
            return result;
        }

        for (String line : Files.readAllLines(statusFile)) {
            String[] parts = line.split("\t");
            if (parts.length < 2) {
                throw new IOException("状态文件格式不正确，缺少状态字段: " + line);
            }
            result.add(new FileStatus(parts[0], "已写入".equals(parts[1])));
        }

        return result;
    }

    private static void saveStatusFile(Path statusFile, List<FileStatus> statusList) throws IOException {
        List<String> lines = statusList.stream()
                .map(fs -> fs.getFilePath() + "\t" + (fs.isProcessed() ? "已写入" : "未写入"))
                .collect(Collectors.toList());

        Files.write(statusFile, lines);
    }
}