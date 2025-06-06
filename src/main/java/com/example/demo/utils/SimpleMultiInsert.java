package com.example.demo.utils;

import com.example.demo.entity.CalculateResult;
import com.example.demo.service.HighSensorRedisService;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class SimpleMultiInsert {

    private static String influxDbOrg = "test";
    private static String influxDbBucket = "test2";

    public static final int BATCH_SIZE = 100000;

    // 线程池配置
//    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final int THREAD_POOL_SIZE = 4;
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private static HighSensorRedisService highSensorRedisService;


    public static void writeDataFromFile(InfluxDBClient client, String filePath) throws IOException, ExecutionException, InterruptedException {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        Map<String, Integer> timeCounts = new HashMap<>(); // 记录每个时间点的计数
        int processedLines = 0;
        int batchCount = 0;
        long startTime = System.currentTimeMillis(); // 记录开始时间
        long t1 = 0;

        // 使用 AtomicLong 替代普通变量
        AtomicLong totalReadTime = new AtomicLong(0); // 读取和拆分数据的总时间
        AtomicLong totalCalculateTime = new AtomicLong(0); // 计算实际值的总时间
        AtomicLong totalBuildProtocolTime = new AtomicLong(0); // 拼接行协议的总时间

        List<Future<List<String>>> futures = new ArrayList<>();
        System.out.println("THREAD_POOL_SIZE : " + THREAD_POOL_SIZE);
        // 从文件路径中提取解调器编号
        String decoderId = filePath.split("\\\\")[2]; // 例如：从 "E:\\decoder\\01\\Wave_20240712_010000.txt" 中提取 "01"
        int decoderNumber = Integer.parseInt(decoderId); // 转换为整数

        // 跳过文件的第一行表头
        reader.readLine();

        // 读取文件并处理
        while ((line = reader.readLine()) != null) {
            long readStartTime = System.currentTimeMillis();

            String[] columns = line.split("\t");
            if (columns.length < 4) {
                continue;
            }

            String baseTime = columns[2].trim(); // 基准时间
            int counter = timeCounts.getOrDefault(baseTime, 0); // 当前时间点的计数

            // 如果当前时间点的数据量已经达到 1000 条，则跳过
            if (counter >= 1000) {
                continue;
            }

            timeCounts.put(baseTime, counter + 1); // 更新计数器

            // 转换基准时间为纳秒时间戳
            long timestampNs = convertTimestamp(baseTime, counter, 1000); // 采样频率为 1000Hz

            // 提取 32 个信道的原始值
            double[] originalValues = new double[32];
            for (int i = 3; i < columns.length && (i - 3) / 3 < 32; i += 3) { // 每隔3列取一个信道
                String value = columns[i].replace("|", "").trim();

                // 如果值为空或仅包含竖线或空格，则跳过该信道
                if (value.isEmpty() || value.equals("|") || value.equals(" ")) {
                    originalValues[(i - 3) / 3] = 0.0; // 默认值为 0.0
                    continue;
                }

                try {
                    originalValues[(i - 3) / 3] = Double.parseDouble(value); // 提取原始值
                } catch (NumberFormatException e) {
                    // 如果数据无法解析为数字，则跳过该信道
                    originalValues[(i - 3) / 3] = 0.0; // 默认值为 0.0
                    continue;
                }
            }

            long readEndTime = System.currentTimeMillis();
            totalReadTime.addAndGet(readEndTime - readStartTime); // 累加读取时间

            // 提交任务到线程池
            Future<List<String>> future = executor.submit(() -> {
                long calculateStartTime = System.currentTimeMillis();

                // 计算实际值
                CalculateResult result = Calculator.calculate(originalValues, decoderNumber);
                double[] actualValues = result.getActualValues();
                Map<String, Double> reviseValues = result.getReviseValues();

                long calculateEndTime = System.currentTimeMillis();
                totalCalculateTime.addAndGet(calculateEndTime - calculateStartTime); // 累加计算时间

                // 创建一个 StringBuilder 来构建包含所有信道数据的行协议
                StringBuilder lineProtocolBuilder = new StringBuilder();
                lineProtocolBuilder.append(String.format("sensor_data,decoder=%d ", decoderNumber)); // 设置解调器编号

                // 处理每个信道的数据
                for (int channel = 0; channel < 32; channel++) {
                    lineProtocolBuilder.append(String.format("Ch%d_ori=%f,", channel + 1, originalValues[channel])); // 原始值
                    lineProtocolBuilder.append(String.format("Ch%d_act=%f,", channel + 1, actualValues[channel])); // 实际值
                }

                // 处理修正值
                for (Map.Entry<String, Double> entry : reviseValues.entrySet()) {
                    lineProtocolBuilder.append(String.format("%s=%f,", entry.getKey(), entry.getValue()));
                }

                long buildProtocolEndTime = System.currentTimeMillis();
                totalBuildProtocolTime.addAndGet(buildProtocolEndTime - calculateEndTime); // 累加拼接行协议时间

                // 去掉最后一个逗号并添加时间戳
                if (lineProtocolBuilder.length() > 0) {
                    lineProtocolBuilder.setLength(lineProtocolBuilder.length() - 1); // 去掉最后一个逗号
                    lineProtocolBuilder.append(String.format(" %d", timestampNs));
                }

                return Collections.singletonList(lineProtocolBuilder.toString());
            });

            futures.add(future);
            processedLines++;

            // 如果批量达到指定大小，写入数据库
            if (processedLines >= BATCH_SIZE) {
                List<String> batchLines = new ArrayList<>();
                for (Future<List<String>> f : futures) {
                    batchLines.addAll(f.get());
                }

                // 输出计时结果
                System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，读取拆分数据时间为：" + (totalReadTime.get() / 1000.0) + " 秒");
                System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，计算实际值时间为：" + (totalCalculateTime.get() / 1000.0) + " 秒");
                System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，拼接行协议时间为：" + (totalBuildProtocolTime.get() / 1000.0) + " 秒");

                long tm = System.currentTimeMillis();
                // 写入数据库
                writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
                batchLines.clear(); // 清空批量数据

                batchCount++;
                long elapsedTime = System.currentTimeMillis() - startTime;
                t1 = t1 + System.currentTimeMillis() - tm;
                System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，写入数据库用时：" + (t1 / 1000.0) + " 秒");
                System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，累计用时：" + (elapsedTime / 1000.0) + " 秒");

                futures.clear();
                processedLines = 0;
            }
        }

        // 处理剩余的批量数据
        if (!futures.isEmpty()) {
            List<String> batchLines = new ArrayList<>();
            for (Future<List<String>> f : futures) {
                batchLines.addAll(f.get());
            }

            // 写入数据库
            writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
            batchCount++;
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，累计用时：" + (elapsedTime / 1000.0) + " 秒");
        }

        reader.close();
        System.out.println("数据写入完成，共处理 " + processedLines + " 条数据。");
        executor.shutdown(); // 关闭线程池
    }

    private static long convertTimestamp(String baseTime, int counter, int frequency) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
            Date date = sdf.parse(baseTime);
            long baseTimeNs = date.getTime() * 1000000; // 转换为纳秒
            long offsetNs = counter * (1000000000L / frequency); // 计算每个采样点的时间偏移量
            return baseTimeNs + offsetNs;
        } catch (Exception e) {
            System.out.println("时间格式转换错误: " + baseTime);
            return 0; // 默认返回 0 时间戳
        }
    }

    private static void writeBatchToFile(List<String> batchLines) throws IOException {
        // 获取文件路径中的目录部分
        String outputFilePath = "E:\\decoder\\01\\0.txt";

        // 打开文件并写入数据
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            for (String line : batchLines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    public static void writeDataFromFile1(InfluxDBClient client, String filePath) throws Exception {
        // 初始化
        int decoderNumber = Integer.parseInt(filePath.split("\\\\")[2]);
        WriteApi writeApi = client.getWriteApi(WriteOptions.builder().batchSize(10000).build());
        File file = new File(filePath);
        long fileSize = file.length();
        long chunkSize = fileSize / THREAD_POOL_SIZE;
        // 多线程处理
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            final int threadNum = i;
            futures.add(executor.submit(() -> {
                List<String> lines = SafeFileProcessor.readFileChunk(filePath,
                        threadNum * chunkSize,
                        (threadNum == THREAD_POOL_SIZE-1) ? fileSize : (threadNum+1)*chunkSize);

                List<String> protocolLines = new ArrayList<>();
                String batchId = null;

                for (int j = 0; j < lines.size(); j++) {
                    String[] cols = lines.get(j).split("\t");
                    if (cols.length < 4) continue;

                    // 初始化批次
                    if (batchId == null) {
                        batchId = filePath + "_" + cols[1].trim();
                        highSensorRedisService.startBatch(batchId);
                    }

                    // 生成行协议
                    String lp = buildLineProtocol(cols,
                            generateNanoTimestamp(cols, j),
                            decoderNumber);
                    protocolLines.add(lp);

                    // 批量写入
                    if (protocolLines.size() >= 1000) {
                        writeToFile(filePath, protocolLines);
                        writeApi.writeRecords(influxDbBucket, influxDbOrg,
                                WritePrecision.NS, protocolLines);
                        protocolLines.clear();
                    }
                }

                // 写入剩余数据
                if (!protocolLines.isEmpty()) {
                    writeToFile(filePath, protocolLines);
                    writeApi.writeRecords(influxDbBucket, influxDbOrg,
                            WritePrecision.NS, protocolLines);
                }

                highSensorRedisService.markBatchSuccess(batchId);
                return null;
            }));
        }

        // 等待完成
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(1000);
        }
    }

    private static String buildLineProtocol(String[] cols, long nanoTimestamp, int decoderNumber) {
        // 提取原始值
        double[] originalValues = new double[32];
        for (int i = 3; i < cols.length && (i-3)/3 < 32; i += 3) {
            try {
                originalValues[(i-3)/3] = Double.parseDouble(cols[i].replace("|","").trim());
            } catch (Exception e) {
                originalValues[(i-3)/3] = 0.0;
            }
        }

        // 计算实际值和修正值
        CalculateResult result = Calculator.calculate(originalValues, decoderNumber);
        double[] actualValues = result.getActualValues();
        Map<String, Double> reviseValues = result.getReviseValues();

        // 构建行协议
        StringBuilder sb = new StringBuilder()
                .append(String.format("sensor_data,decoder=%d ", decoderNumber));

        // 添加原始值和实际值
        for (int channel = 0; channel < 32; channel++) {
            sb.append(String.format("Ch%d_ori=%f,", channel+1, originalValues[channel]))
                    .append(String.format("Ch%d_act=%f,", channel+1, actualValues[channel]));
        }

        // 添加修正值
        reviseValues.forEach((k,v) -> sb.append(String.format("%s=%f,", k, v)));

        // 移除末尾逗号并添加时间戳
        if (sb.length() > 0) {
            sb.setLength(sb.length()-1);
            sb.append(" ").append(nanoTimestamp);
        }

        return sb.toString();
    }

    private static long generateNanoTimestamp(String[] cols, int lineIndex) {
        long secTimestamp = SafeFileProcessor.parseSecondTimestamp(cols[2]);

        // 首秒数据特殊处理（避免序号从0开始）
        if (lineIndex == 0) {
            return secTimestamp * 1_000_000_000L + 500_000; // 默认放在秒中间
        }

        // 正常数据：1ms间隔
        return secTimestamp * 1_000_000_000L + (lineIndex % 1000) * 1_000_000;
    }

    public static void writeToFile(String filePath, List<String> lines) {
        Path logPath = Paths.get(filePath).getParent().resolve("line_protocol.log");
        try (BufferedWriter writer = Files.newBufferedWriter(logPath,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("日志写入失败: " + e.getMessage());
        }
    }
}