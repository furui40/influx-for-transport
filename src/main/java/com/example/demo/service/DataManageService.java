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

    @Value("${data.highSensor-dir}")
    private String highSensorDir;

    @Value("${data.dynamicWeighing-dir}")
    private String dynamicWeighingDir;

    @Value("${data.weather-dir}")
    private String weatherDir;

    @Value("${data.subside-dir}")
    private String subsideDir;

    @Value("${data.waterPressure-dir}")
    private String waterPressureDir;

    @Value("${data.humiture-dir}")
    private String humitureDir;

    public List<FileStatus> getOrCreateStatus(String dataType) throws IOException {
        Path statusFile = getStatusFilePath(dataType);

        // 如果是weather数据类型，先执行xls转xlsx转换脚本
        if ("weather".equals(dataType)) {
            convertXlsToXlsx(weatherDir);
        }

        if (!Files.exists(statusFile)) {
            return createStatusFile(dataType);
        }

        Map<String, Boolean> existingStatus = loadStatusMap(statusFile);

        List<FileStatus> currentFiles = scanFilesRecursively(dataType);

        List<FileStatus> mergedStatus = new ArrayList<>();
        for (FileStatus fileStatus : currentFiles) {
            Boolean existingState = existingStatus.get(fileStatus.getFilePath());
            mergedStatus.add(new FileStatus(
                    fileStatus.getFilePath(),
                    Boolean.TRUE.equals(existingState)
            ));
        }

        saveStatusList(statusFile, mergedStatus);

        return mergedStatus;
    }


    public void processFiles(InfluxDBClient influxDBClient, String dataType, List<String> filePaths) throws Exception {
        Path statusFile = getStatusFilePath(dataType);
        Map<String, Boolean> statusMap = loadStatusMap(statusFile);

        for (String filePath : filePaths) {
            try {
                processSingleFile(influxDBClient, dataType, filePath);
                statusMap.put(filePath, true);
            } catch (Exception e) {
                statusMap.put(filePath, false);
                throw new RuntimeException("处理文件失败: " + filePath, e);
            }
        }

        saveStatusMap(statusFile, statusMap);
    }

    private void processSingleFile(InfluxDBClient influxDBClient, String dataType, String filePath) throws Exception {
        switch (dataType) {
            case "dynamicWeighing":
                DynamicWeighingService.processFile(influxDBClient, filePath);
                break;
            case "weather":
                WeatherService.processFile(influxDBClient, filePath);
                break;
            case "subside":
                JinMaDataService.processFile(influxDBClient, filePath,"subside");
                break;
            case "waterPressure":
                JinMaDataService.processFile(influxDBClient, filePath,"waterPressure");
                break;
            case "humiture":
                JinMaDataService.processFile(influxDBClient, filePath,"humiture");
                break;
            default:
                throw new IllegalArgumentException("不支持的数据类型: " + dataType);
        }
    }

    private Path getStatusFilePath(String dataType) {
        return Paths.get(baseDir, dataType + "_status.txt");
    }

    private List<FileStatus> createStatusFile(String dataType) throws IOException {
        List<FileStatus> statusList = scanFilesRecursively(dataType);
        Path statusFile = getStatusFilePath(dataType);
        saveStatusList(statusFile, statusList);
        return statusList;
    }

    private List<FileStatus> scanFilesRecursively(String dataType) throws IOException {
        String dataDir = getDataDirectory(dataType);
        Path dir = Paths.get(dataDir);

        List<FileStatus> statusList = new ArrayList<>();
        scanDirectoryRecursively(dir, statusList);

        return statusList;
    }

    private void scanDirectoryRecursively(Path directory, List<FileStatus> statusList) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    scanDirectoryRecursively(path, statusList);
                } else if (!path.getFileName().toString().endsWith("_status.txt")) {
                    statusList.add(new FileStatus(path.toString(), false));
                }
            }
        }
    }

    private String getDataDirectory(String dataType) {
        switch (dataType) {
            case "dynamicWeighing":
                return dynamicWeighingDir;
            case "weather":
                return weatherDir;
            case "subside":
                return subsideDir;
            case "waterPressure":
                return waterPressureDir;
            case "humiture":
                return humitureDir;
            case "highSensor":
                return highSensorDir;
            default:
                throw new IllegalArgumentException("不支持的数据类型: " + dataType);
        }
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

        Files.write(statusFile, lines, StandardCharsets.UTF_8);
    }

    private void saveStatusList(Path statusFile, List<FileStatus> statusList) throws IOException {
        List<String> lines = statusList.stream()
                .map(fs -> fs.getFilePath() + "\t" + (fs.isProcessed() ? "已写入" : "未写入"))
                .collect(Collectors.toList());

        Files.createDirectories(statusFile.getParent());
        Files.write(statusFile, lines, StandardCharsets.UTF_8);
    }

    // 新增方法：执行xls转xlsx的Python脚本
    private void convertXlsToXlsx(String directory) throws IOException {
        String pythonScript = "E:\\project\\Platform\\script\\XlsConvertToXlsx.py";
        String[] command = new String[]{"python", pythonScript, directory};

        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("XLS to XLSX conversion failed with exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("XLS to XLSX conversion was interrupted", e);
        }
    }
}