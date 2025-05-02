package com.example.demo.utils;

import com.example.demo.entity.CalculateResult;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class MultiInsert {
    private static String influxDbOrg = "test";
    private static String influxDbBucket = "test7";
    private static final int BUFFER_SIZE = 10500;
    private static final int SECONDS_PER_BATCH = 10;
    private static final int MAX_PER_SECOND = 1000;
    private static final int BATCH_SIZE = 10000;
    private static final int THREAD_POOL_SIZE = 8;

    // 计时统计变量
    private static AtomicLong totalReadTime = new AtomicLong(0);
    private static AtomicLong totalCalcTime = new AtomicLong(0);
    private static AtomicLong totalProtocolTime = new AtomicLong(0);
    private static AtomicLong totalWriteTime = new AtomicLong(0);
    private static AtomicLong totalProcessTime = new AtomicLong(0);
    private static AtomicLong processedRecords = new AtomicLong(0);
    private static AtomicLong processedBatches = new AtomicLong(0);

    private static ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static List<Future<?>> futures = new CopyOnWriteArrayList<>();
    private static List<String> buffer = Collections.synchronizedList(new ArrayList<>(BUFFER_SIZE));
    public static void writeDataFromFile1(InfluxDBClient client, String filePath) throws IOException {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        String decoderId = filePath.split("\\\\")[2];
        int decoderNumber = Integer.parseInt(decoderId);

        reader.readLine();

        String currentSecond = null;
        String line;

        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 4) continue;

            String baseTime = columns[2].trim();

            synchronized(buffer) {
                if (currentSecond != null && !baseTime.equals(currentSecond)) {
                    processSecondData(new ArrayList<>(buffer), currentSecond, decoderNumber, writeApiBlocking);
                    buffer.clear();
                    currentSecond = baseTime;
                }

                if (currentSecond == null) {
                    currentSecond = baseTime;
                }

                buffer.add(line);

                if (buffer.size() >= BUFFER_SIZE) {
                    processSecondData(new ArrayList<>(buffer), currentSecond, decoderNumber, writeApiBlocking);
                    buffer.clear();
                }
            }
        }

        synchronized(buffer) {
            if (!buffer.isEmpty()) {
                processSecondData(new ArrayList<>(buffer), currentSecond, decoderNumber, writeApiBlocking);
            }
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("任务被中断: " + e.getMessage());
            } catch (ExecutionException e) {
                System.err.println("任务执行异常: " + e.getCause().getMessage());
            }
        }

        reader.close();
        executor.shutdown();
    }

    public static void writeDataFromFile2(InfluxDBClient client, String filePath) throws IOException {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        String decoderId = filePath.split("\\\\")[2];
        int decoderNumber = Integer.parseInt(decoderId);
        reader.readLine(); // 跳过表头

        List<String> buffer = new ArrayList<>(BUFFER_SIZE);
        String firstSecond = null;
        Set<String> containedSeconds = new HashSet<>();
        String line;

        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 4) continue;

            String currentSecond = columns[2].trim();

            // 初始化第一个秒
            if (firstSecond == null) {
                firstSecond = currentSecond;
                containedSeconds.add(currentSecond);
            }

            // 当满足以下条件时处理数据：
            // 1. 缓冲区已满
            // 2. 包含超过10个不同的秒
            // 3. 当前秒与首个秒的时间差超过10秒
            if (buffer.size() >= BUFFER_SIZE ||
                    containedSeconds.size() >= SECONDS_PER_BATCH ||
                    isOverTenSeconds(firstSecond, currentSecond)) {

                processTenSecondsData(new ArrayList<>(buffer), decoderNumber, writeApiBlocking);

                // 重置缓冲区
                buffer.clear();
                containedSeconds.clear();
                firstSecond = currentSecond;
            }

            buffer.add(line);
            containedSeconds.add(currentSecond);
        }

        // 处理剩余数据
        if (!buffer.isEmpty()) {
            processTenSecondsData(buffer, decoderNumber, writeApiBlocking);
        }

        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        reader.close();
        executor.shutdown();
    }

    public static void writeDataFromFile3(InfluxDBClient client, String filePath) throws IOException {
        long programStartTime = System.currentTimeMillis();
        System.out.println("开始处理文件: " + filePath);

        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        // 从文件路径中提取解调器编号
        String decoderId = filePath.split("\\\\")[2];
        int decoderNumber = Integer.parseInt(decoderId);

        // 跳过文件的第一行表头
        reader.readLine();

        List<String> buffer = new ArrayList<>(BUFFER_SIZE);
        String firstSecond = null;
        Set<String> containedSeconds = new HashSet<>();
        String line;
        int lineCount = 0;

        // 主处理循环
        while ((line = reader.readLine()) != null) {
            lineCount++;
            String[] columns = line.split("\t");
            if (columns.length < 4) continue;

            String currentSecond = columns[2].trim();

            // 初始化第一个秒
            if (firstSecond == null) {
                firstSecond = currentSecond;
                containedSeconds.add(currentSecond);
            }

            // 检查是否需要处理当前缓冲区
            if (buffer.size() >= BUFFER_SIZE ||
                    containedSeconds.size() >= SECONDS_PER_BATCH ||
                    isOverTenSeconds(firstSecond, currentSecond)) {

                System.out.printf("处理批次 [行数:%d, 秒数:%d, 起始时间:%s]%n",
                        buffer.size(), containedSeconds.size(), firstSecond);

                processTenSecondsData3(new ArrayList<>(buffer), decoderNumber, writeApiBlocking);

                // 重置缓冲区
                buffer.clear();
                containedSeconds.clear();
                firstSecond = currentSecond;
            }

            buffer.add(line);
            containedSeconds.add(currentSecond);

            // 进度输出
            if (lineCount % 10000 == 0) {
                System.out.printf("已读取 %,d 行, 当前缓冲区 %,d 行%n", lineCount, buffer.size());
            }
        }

        // 处理剩余数据
        if (!buffer.isEmpty()) {
            System.out.printf("处理最后批次 [行数:%d, 秒数:%d, 起始时间:%s]%n",
                    buffer.size(), containedSeconds.size(), firstSecond);
            processTenSecondsData3(buffer, decoderNumber, writeApiBlocking);
        }

        // 等待所有任务完成
        System.out.println("等待所有处理任务完成...");
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("处理被中断: " + e.getMessage());
            } catch (ExecutionException e) {
                System.err.println("处理出错: " + e.getCause().getMessage());
            }
        }

        reader.close();
        executor.shutdown();

        // 输出最终统计信息
        printFinalStatistics(programStartTime);
    }

    private static void processSecondData(List<String> lines, String second, int decoderNumber, WriteApiBlocking writeApiBlocking) {
        // 提交处理任务到线程池
        Future<?> future = executor.submit(() -> {
            List<String> batchLines = new ArrayList<>(BATCH_SIZE);
            List<String> fileLines = new ArrayList<>(MAX_PER_SECOND); // 用于写入文件的内容
            int counter = 0;

            for (String line : lines) {
                String[] columns = line.split("\t");
                if (columns.length < 4) continue;

                String baseTime = columns[2].trim();
                if (!baseTime.equals(second)) continue;

                // 限制每秒最多1000条数据
                if (counter >= MAX_PER_SECOND) continue;
                counter++;

                long timestampNs = convertTimestamp(baseTime, counter, 1000);

                // 提取32个信道的原始值
                double[] originalValues = new double[32];
                for (int i = 3; i < columns.length && (i - 3) / 3 < 32; i += 3) {
                    String value = columns[i].replace("|", "").trim();
                    try {
                        originalValues[(i - 3) / 3] = value.isEmpty() ? 0.0 : Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        originalValues[(i - 3) / 3] = 0.0;
                    }
                }

                // 计算实际值和修正值
                CalculateResult result = Calculator.calculate(originalValues, decoderNumber);
                double[] actualValues = result.getActualValues();
                Map<String, Double> reviseValues = result.getReviseValues();

                // 构建行协议
                StringBuilder lineProtocol = new StringBuilder();
                lineProtocol.append(String.format("sensor_data,decoder=%d ", decoderNumber));

                for (int channel = 0; channel < 32; channel++) {
                    lineProtocol.append(String.format("Ch%d_ori=%f,", channel + 1, originalValues[channel]));
                    lineProtocol.append(String.format("Ch%d_act=%f,", channel + 1, actualValues[channel]));
                }

                for (Map.Entry<String, Double> entry : reviseValues.entrySet()) {
                    lineProtocol.append(String.format("%s=%f,", entry.getKey(), entry.getValue()));
                }

                if (lineProtocol.length() > 0) {
                    lineProtocol.setLength(lineProtocol.length() - 1);
                    lineProtocol.append(String.format(" %d", timestampNs));

                    String protocolStr = lineProtocol.toString();
                    batchLines.add(protocolStr);
                    fileLines.add(protocolStr); // 添加到文件写入列表

                    // 批量写入
                    if (batchLines.size() >= BATCH_SIZE) {
                        // 先写入文件
//                        writeBatchToFile(fileLines, second, decoderNumber);
                        System.out.println("succeed: " + second);

                        // 再写入数据库
                        writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
                        batchLines.clear();
                    }
                }
            }

            // 写入剩余数据
            if (!batchLines.isEmpty()) {
                // 先写入文件
//                writeBatchToFile(fileLines, second, decoderNumber);
                System.out.println("succeed: " + second);

                // 再写入数据库
                writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
            }
        });

        futures.add(future);
    }

    private static void processTenSecondsData(List<String> dataBatch, int decoderNumber, WriteApiBlocking writeApiBlocking) {
        futures.add(executor.submit(() -> {
            // 按秒分组处理
            Map<String, List<String>> groupedData = new LinkedHashMap<>();

            // 第一步：按秒分组
            for (String line : dataBatch) {
                String[] columns = line.split("\t");
                if (columns.length < 4) continue;
                String second = columns[2].trim();
                groupedData.computeIfAbsent(second, k -> new ArrayList<>()).add(line);
            }

            // 第二步：处理每个秒的数据
            for (Map.Entry<String, List<String>> entry : groupedData.entrySet()) {
                String second = entry.getKey();
                List<String> lines = entry.getValue();

                List<String> batchLines = new ArrayList<>(BATCH_SIZE);
                List<String> fileLines = new ArrayList<>(MAX_PER_SECOND);
                int counter = 0;

                for (String line : lines) {
                    String[] columns = line.split("\t");
                    if (columns.length < 4) continue;

                    String baseTime = columns[2].trim();
                    if (!baseTime.equals(second)) continue;

                    // 限制每秒最多1000条数据
                    if (counter >= MAX_PER_SECOND) continue;
                    counter++;

                    long timestampNs = convertTimestamp(baseTime, counter, 1000);

                    // 提取32个信道的原始值
                    double[] originalValues = new double[32];
                    for (int i = 3; i < columns.length && (i - 3) / 3 < 32; i += 3) {
                        String value = columns[i].replace("|", "").trim();
                        try {
                            originalValues[(i - 3) / 3] = value.isEmpty() ? 0.0 : Double.parseDouble(value);
                        } catch (NumberFormatException e) {
                            originalValues[(i - 3) / 3] = 0.0;
                        }
                    }

                    // 计算实际值和修正值
                    CalculateResult result = Calculator.calculate(originalValues, decoderNumber);
                    double[] actualValues = result.getActualValues();
                    Map<String, Double> reviseValues = result.getReviseValues();

                    // 构建行协议
                    StringBuilder lineProtocol = new StringBuilder();
                    lineProtocol.append(String.format("sensor_data,decoder=%d ", decoderNumber));

                    for (int channel = 0; channel < 32; channel++) {
                        lineProtocol.append(String.format("Ch%d_ori=%f,", channel + 1, originalValues[channel]));
                        lineProtocol.append(String.format("Ch%d_act=%f,", channel + 1, actualValues[channel]));
                    }

                    for (Map.Entry<String, Double> entry2 : reviseValues.entrySet()) {
                        lineProtocol.append(String.format("%s=%f,", entry2.getKey(), entry2.getValue()));
                    }

                    if (lineProtocol.length() > 0) {
                        lineProtocol.setLength(lineProtocol.length() - 1);
                        lineProtocol.append(String.format(" %d", timestampNs));

                        String protocolStr = lineProtocol.toString();
                        batchLines.add(protocolStr);
                        fileLines.add(protocolStr); // 添加到文件写入列表

                        // 批量写入
                        if (batchLines.size() >= BATCH_SIZE) {
                            // 先写入文件
//                        writeBatchToFile(fileLines, second, decoderNumber);
                            System.out.println("succeed: " + second);

                            // 再写入数据库
                            writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
                            batchLines.clear();
                        }
                    }
                }

                // 写入剩余数据
                if (!batchLines.isEmpty()) {
//                    writeBatchToFile(fileLines, second, decoderNumber);
                    System.out.println("succeed: " + second);
                    writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
                }
            }
        }));
    }

    // 单秒写入
//    private static void processTenSecondsData3(List<String> dataBatch, int decoderNumber,
//                                               WriteApiBlocking writeApiBlocking) {
//        futures.add(executor.submit(() -> {
//            long batchStartTime = System.currentTimeMillis();
//            long readTime = 0, calcTime = 0, protocolTime = 0, writeTime = 0;
//            int recordsProcessed = 0;
//
//            try {
//                // 阶段1: 数据读入和分组
//                long start = System.currentTimeMillis();
//                Map<String, List<String>> groupedData = groupDataBySecond(dataBatch);
//                readTime = System.currentTimeMillis() - start;
//
//                // 阶段2-4: 处理每个秒的数据
//                for (Map.Entry<String, List<String>> entry : groupedData.entrySet()) {
//                    String second = entry.getKey();
//                    List<String> lines = entry.getValue();
//
//                    // 限制每秒最多处理MAX_PER_SECOND条
//                    int linesToProcess = Math.min(lines.size(), MAX_PER_SECOND);
//                    List<String> batchLines = new ArrayList<>(BATCH_SIZE);
//                    List<String> fileLines = new ArrayList<>(linesToProcess);
//
//                    for (int i = 0; i < linesToProcess; i++) {
//                        String line = lines.get(i);
//                        String[] columns = line.split("\t");
//
//                        // 阶段2: 数据计算
//                        long calcStart = System.currentTimeMillis();
//                        double[] originalValues = extractOriginalValues(columns);
//                        CalculateResult result = Calculator.calculate(originalValues, decoderNumber);
//                        calcTime += System.currentTimeMillis() - calcStart;
//
//                        // 阶段3: 行协议拼接
//                        long protocolStart = System.currentTimeMillis();
//                        String lineProtocol = buildLineProtocol(
//                                decoderNumber,
//                                originalValues,
//                                result.getActualValues(),
//                                result.getReviseValues(),
//                                convertTimestamp(second, i, 1000)
//                        );
//                        protocolTime += System.currentTimeMillis() - protocolStart;
//
//                        batchLines.add(lineProtocol);
//                        fileLines.add(lineProtocol);
//                        recordsProcessed++;
//
//                        // 阶段4: 批量写入
//                        if (batchLines.size() >= BATCH_SIZE) {
//                            long writeStart = System.currentTimeMillis();
////                            writeBatchToFile(fileLines, second, decoderNumber);
//                            System.out.println("succeed: " + second);
//                            writeApiBlocking.writeRecord(
//                                    influxDbBucket,
//                                    influxDbOrg,
//                                    WritePrecision.NS,
//                                    String.join("\n", batchLines)
//                            );
//                            writeTime += System.currentTimeMillis() - writeStart;
//                            batchLines.clear();
//                        }
//                    }
//
//                    // 写入剩余数据
//                    if (!batchLines.isEmpty()) {
//                        long writeStart = System.currentTimeMillis();
////                        writeBatchToFile(fileLines, second, decoderNumber);
//                        System.out.println("succeed: " + second);
//                        writeApiBlocking.writeRecord(
//                                influxDbBucket,
//                                influxDbOrg,
//                                WritePrecision.NS,
//                                String.join("\n", batchLines)
//                        );
//                        writeTime += System.currentTimeMillis() - writeStart;
//                    }
//                }
//
//                // 更新统计
//                synchronized (Test2.class) {
//                    totalReadTime.addAndGet(readTime);
//                    totalCalcTime.addAndGet(calcTime);
//                    totalProtocolTime.addAndGet(protocolTime);
//                    totalWriteTime.addAndGet(writeTime);
//                    totalProcessTime.addAndGet(System.currentTimeMillis() - batchStartTime);
//                    processedRecords.addAndGet(recordsProcessed);
//                    processedBatches.incrementAndGet();
//                }
//
//                System.out.printf("完成批次处理 [记录数:%d, 耗时:%.3fs]%n",
//                        recordsProcessed, (System.currentTimeMillis() - batchStartTime) / 1000.0);
//
//            } catch (Exception e) {
//                System.err.println("处理十秒数据块时出错: " + e.getMessage());
//                e.printStackTrace();
//            }
//        }));
//    }

    // 十秒写入
    private static void processTenSecondsData3(List<String> dataBatch, int decoderNumber,
                                               WriteApiBlocking writeApiBlocking) {
        futures.add(executor.submit(() -> {
            long batchStartTime = System.currentTimeMillis();
            long readTime = 0, calcTime = 0, protocolTime = 0, writeTime = 0;
            int recordsProcessed = 0;

            // 用于累积所有秒的数据
            List<String> allBatchLines = new ArrayList<>(BATCH_SIZE * SECONDS_PER_BATCH);
            Map<String, List<String>> secondToFileLines = new HashMap<>();

            try {
                // 阶段1: 数据读入和分组
                long start = System.currentTimeMillis();
                Map<String, List<String>> groupedData = groupDataBySecond(dataBatch);
                readTime = System.currentTimeMillis() - start;

                // 阶段2-3: 处理每个秒的数据(只计算和构建协议，不写入)
                for (Map.Entry<String, List<String>> entry : groupedData.entrySet()) {
                    String second = entry.getKey();
                    List<String> lines = entry.getValue();

                    // 为当前秒创建文件内容存储
                    List<String> fileLines = new ArrayList<>(Math.min(lines.size(), MAX_PER_SECOND));
                    secondToFileLines.put(second, fileLines);

                    // 限制每秒最多处理MAX_PER_SECOND条
                    int linesToProcess = Math.min(lines.size(), MAX_PER_SECOND);

                    for (int i = 0; i < linesToProcess; i++) {
                        String line = lines.get(i);
                        String[] columns = line.split("\t");

                        // 阶段2: 数据计算
                        long calcStart = System.currentTimeMillis();
                        double[] originalValues = extractOriginalValues(columns);
                        CalculateResult result = Calculator.calculate(originalValues, decoderNumber);
                        calcTime += System.currentTimeMillis() - calcStart;

                        // 阶段3: 行协议拼接
                        long protocolStart = System.currentTimeMillis();
                        String lineProtocol = buildLineProtocol(
                                decoderNumber,
                                originalValues,
                                result.getActualValues(),
                                result.getReviseValues(),
                                convertTimestamp(second, i, 1000)
                        );
                        protocolTime += System.currentTimeMillis() - protocolStart;

                        allBatchLines.add(lineProtocol);
                        fileLines.add(lineProtocol);
                        recordsProcessed++;
                    }
                }

                // 阶段4: 批量写入数据库和文件(所有秒数据一起写入)
                long writeStart = System.currentTimeMillis();

                // 先写入所有文件
                for (Map.Entry<String, List<String>> entry : secondToFileLines.entrySet()) {
                    System.out.println("succeed: " + entry.getKey());
//                    writeBatchToFile(entry.getValue(), entry.getKey(), decoderNumber);
                }

                // 再批量写入数据库
                if (!allBatchLines.isEmpty()) {
                    // 分批写入数据库，避免单次写入过大
                    for (int i = 0; i < allBatchLines.size(); i += BATCH_SIZE) {
                        int end = Math.min(i + BATCH_SIZE, allBatchLines.size());
                        List<String> batch = allBatchLines.subList(i, end);
                        writeApiBlocking.writeRecord(
                                influxDbBucket,
                                influxDbOrg,
                                WritePrecision.NS,
                                String.join("\n", batch)
                        );
                    }
                }
                writeTime = System.currentTimeMillis() - writeStart;

                // 更新统计
                synchronized (MultiInsert.class) {
                    totalReadTime.addAndGet(readTime);
                    totalCalcTime.addAndGet(calcTime);
                    totalProtocolTime.addAndGet(protocolTime);
                    totalWriteTime.addAndGet(writeTime);
                    totalProcessTime.addAndGet(System.currentTimeMillis() - batchStartTime);
                    processedRecords.addAndGet(recordsProcessed);
                    processedBatches.incrementAndGet();
                }

                System.out.printf("完成批次处理 [记录数:%d, 耗时:%.3fs(计算:%.3fs, 写入:%.3fs)]%n",
                        recordsProcessed,
                        (System.currentTimeMillis() - batchStartTime) / 1000.0,
                        (calcTime + protocolTime) / 1000.0,
                        writeTime / 1000.0);

            } catch (Exception e) {
                System.err.println("处理十秒数据块时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }

    private static void writeBatchToFile(List<String> lines, String second, int decoderNumber) {
        // 替换文件名中的非法字符
        String safeSecond = second.replaceAll("[:,\\\\/\\*\\?\"<>\\|]", "_");

        String dirPath = String.format("E:\\decoder\\test%02d", decoderNumber); // 使用参数中的decoderNumber
        String filePath = String.format("%s\\%s.txt", dirPath, safeSecond);

        // 创建目录（如果不存在）
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("创建目录失败: " + dirPath);
                return;
            }
        }

        // 写入文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("成功写入文件: " + filePath); // 添加成功日志
        } catch (IOException e) {
            System.err.println("写入文件失败: " + filePath);
            e.printStackTrace(); // 打印完整异常堆栈
        }
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

    private static boolean isOverTenSeconds(String firstSecond, String currentSecond) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
            Date first = sdf.parse(firstSecond);
            Date current = sdf.parse(currentSecond);
            return (current.getTime() - first.getTime()) > SECONDS_PER_BATCH * 1000;
        } catch (ParseException e) {
            return false;
        }
    }

    private static Map<String, List<String>> groupDataBySecond(List<String> dataBatch) {
        Map<String, List<String>> groupedData = new LinkedHashMap<>();
        for (String line : dataBatch) {
            String[] columns = line.split("\t");
            if (columns.length < 4) continue;
            String second = columns[2].trim();
            groupedData.computeIfAbsent(second, k -> new ArrayList<>()).add(line);
        }
        return groupedData;
    }

    private static double[] extractOriginalValues(String[] columns) {
        double[] originalValues = new double[32];
        for (int i = 3; i < columns.length && (i - 3) / 3 < 32; i += 3) {
            String value = columns[i].replace("|", "").trim();
            try {
                originalValues[(i - 3) / 3] = value.isEmpty() ? 0.0 : Double.parseDouble(value);
            } catch (NumberFormatException e) {
                originalValues[(i - 3) / 3] = 0.0;
            }
        }
        return originalValues;
    }

    private static String buildLineProtocol(int decoderNumber, double[] originalValues,
                                            double[] actualValues, Map<String, Double> reviseValues,
                                            long timestampNs) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("sensor_data,decoder=%d ", decoderNumber));

        // 添加原始值和实际值
        for (int channel = 0; channel < 32; channel++) {
            sb.append(String.format("Ch%d_ori=%f,", channel + 1, originalValues[channel]));
            sb.append(String.format("Ch%d_act=%f,", channel + 1, actualValues[channel]));
        }

        // 添加修正值
        for (Map.Entry<String, Double> entry : reviseValues.entrySet()) {
            sb.append(String.format("%s=%f,", entry.getKey(), entry.getValue()));
        }

        // 移除末尾逗号并添加时间戳
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            sb.append(String.format(" %d", timestampNs));
        }

        return sb.toString();
    }

    private static void printFinalStatistics(long programStartTime) {
        long totalTime = System.currentTimeMillis() - programStartTime;
        long otherTime = totalProcessTime.get() - totalReadTime.get() -
                totalCalcTime.get() - totalProtocolTime.get() - totalWriteTime.get();

        System.out.println("\n============= 处理统计 =============");
        System.out.printf("总处理记录数: %,d 条\n", processedRecords.get());
        System.out.printf("总处理批次数: %,d 个\n", processedBatches.get());
        System.out.printf("总用时: %.3f 秒\n", totalTime / 1000.0);
        System.out.printf("平均吞吐量: %.1f 条/秒\n",
                processedRecords.get() / (totalTime / 1000.0));

        System.out.println("\n各阶段耗时统计:");
        System.out.println("--------------------------------");
        System.out.printf("| %-15s | %9s | %6s |\n", "阶段", "耗时(秒)", "占比(%)");
        System.out.println("--------------------------------");
        printStatRow("数据读入", totalReadTime.get() / THREAD_POOL_SIZE, totalTime);
        printStatRow("数据计算", totalCalcTime.get() / THREAD_POOL_SIZE, totalTime);
        printStatRow("协议拼接", totalProtocolTime.get() / THREAD_POOL_SIZE, totalTime);
        printStatRow("数据写入", totalWriteTime.get() / THREAD_POOL_SIZE, totalTime);
        printStatRow("其他处理", otherTime, totalTime);
        System.out.println("--------------------------------");
        System.out.printf("| %-15s | %9.3f | %6.1f |\n", "总计",
                totalProcessTime.get() / 1000.0 / THREAD_POOL_SIZE, 100.0);
        System.out.println("================================");
    }

    private static void printStatRow(String name, long timeMs, long totalTimeMs) {
        System.out.printf("| %-15s | %9.3f | %6.1f |\n",
                name, timeMs / 1000.0, (timeMs * 100.0 / totalTimeMs));
    }
}
