package com.example.demo.service;

import com.example.demo.entity.FileStatus;
import com.influxdb.client.InfluxDBClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataManageService {

    @Value("${data.base-dir:/data}")
    private String baseDir;

    public List<FileStatus> getOrCreateStatus(String dataType) throws IOException {
        Path statusFile = getStatusFilePath(dataType);

        if (!Files.exists(statusFile)) {
            System.out.println("file not exists");
            return createStatusFile(dataType);
        }

        return Files.readAllLines(statusFile).stream()
                .map(line -> {
                    String[] parts = line.split("\t");
                    return new FileStatus(parts[0], "已写入".equals(parts[1]));
                })
                .collect(Collectors.toList());
    }

    public void processFiles(InfluxDBClient influxDBClient, String dataType, List<String> filePaths) throws Exception {
        Path statusFile = getStatusFilePath(dataType);
        Map<String, Boolean> statusMap = loadStatusMap(statusFile);

        // 处理文件
        for (String filePath : filePaths) {
            try {
                processSingleFile(influxDBClient, dataType, filePath);
                statusMap.put(filePath, true);
            } catch (Exception e) {
                statusMap.put(filePath, false);
                throw new RuntimeException("处理文件失败: " + filePath, e);
            }
        }

        // 更新状态
        saveStatusMap(statusFile, statusMap);
    }

    private void processSingleFile(InfluxDBClient influxDBClient, String dataType, String filePath) throws Exception {
        switch (dataType) {
            case "dynamic_weighing":
                DynamicWeighingService.processFile(influxDBClient, filePath);
                break;
            // 添加其他数据类型处理
            // case "temperature":
            //     TemperatureService.processFile(influxDBClient, filePath);
            //     break;
            default:
                throw new IllegalArgumentException("不支持的数据类型: " + dataType);
        }
    }

    private Path getStatusFilePath(String dataType) {
        System.out.println("baseDir:" + baseDir);
        System.out.println("StatusFilePath: " + Paths.get(baseDir, dataType + "_status.txt"));
        return Paths.get(baseDir, dataType + "_status.txt");
    }

    private List<FileStatus> createStatusFile(String dataType) throws IOException {
        // 使用双反斜杠或正斜杠处理Windows路径
        Path dir = Paths.get("E:/data/2024二三季度动态称重/动态称重4-9");

        System.out.println("createStatusFile:" + dir);
        List<FileStatus> statusList = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir,
                path -> !path.getFileName().toString().equals("status.txt"))) {
            for (Path file : stream) {
                statusList.add(new FileStatus(file.toString(), false));
            }
        }

        // 写入状态文件（保存到./data/dynamic_weighing_status.txt）
        List<String> lines = statusList.stream()
                .map(fs -> fs.getFilePath() + "\t未写入")
                .collect(Collectors.toList());

        // 确保目标目录存在
        Path statusFile = getStatusFilePath(dataType);
        Files.createDirectories(statusFile.getParent());

        Files.write(statusFile, lines, StandardCharsets.UTF_8);
        return statusList;
    }


    private Map<String, Boolean> loadStatusMap(Path statusFile) throws IOException {
        if (!Files.exists(statusFile)) {
            return new HashMap<>();
        }

        return Files.readAllLines(statusFile).stream()
                .collect(Collectors.toMap(
                        line -> line.split("\t")[0],
                        line -> "已写入".equals(line.split("\t")[1]))
                );
    }

    private void saveStatusMap(Path statusFile, Map<String, Boolean> statusMap) throws IOException {
        List<String> lines = statusMap.entrySet().stream()
                .map(entry -> entry.getKey() + "\t" + (entry.getValue() ? "已写入" : "未写入"))
                .collect(Collectors.toList());

        Files.write(statusFile, lines);
    }

}