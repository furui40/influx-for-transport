package com.example.demo.utils;

import com.example.demo.entity.CalculateResult;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;


import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class DBUtilInsert {

    private static String influxDbOrg = "test";

    private static String influxDbBucket = "test8";


    public static final int BATCH_SIZE = 50000;

    public static final int MAX_CHANNELS = 32; // 最大信道数量
    private static final int SAMPLING_FREQUENCY_HZ = 1000; // 采样频率 (Hz)

//    public static void writeData(InfluxDBClient client, List<MonitorData> monitorData) {
//        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
//
//        // 按照批次拆分数据并写入
//        List<MonitorData> batch = new ArrayList<>();
//        for (int i = 0; i < monitorData.size(); i++) {
//            batch.add(monitorData.get(i));
//
//            // 当批次大小达到设定值时，执行写入操作
//            if (batch.size() >= BATCH_SIZE || i == monitorData.size() - 1) {
//                // 写入当前批次的数据
//                writeApiBlocking.writeMeasurements("test2", "test", WritePrecision.NS, batch);
//                log.info("Batch {} written successfully, size: {}", i / BATCH_SIZE + 1, batch.size());
//
//                // 清空批次
//                batch.clear();
//            }
//        }
//
//        log.info("All data written successfully to InfluxDB.");
//    }
//
//
//    public static List<MonitorData> generateTestData() {
//        // 创建模拟的传感器数据
//        List<MonitorData> monitorDataList = new ArrayList<>();
//
//        // 获取当前时间作为第一个时间戳
//        Instant time = Instant.now().minus(Duration.ofHours(2400));
//
//        // 随机数生成器，用于生成时间间隔
//        Random random = new Random();
//
//        // 创建 5 条模拟数据
//        for (int i = 0; i < 10000; i++) {
//            // 随机生成1到1000毫秒之间的时间间隔
//            long randomInterval = 1 + random.nextInt(10);
//
//            // 设置时间戳为当前时间 + 随机时间间隔
//            time = time.plusMillis(randomInterval);
//
//            // 创建 MonitorData 对象并设置值
//            MonitorData data = new MonitorData()
//                    .setDecoderId("1")   // 设置解调器ID
//                    .setChannelId("ch1")   // 设置信道ID
//                    .setLocationTime(time)  // 设置时间，间隔随机
//                    .setOriginalValue(Math.random() * 100)   // 随机生成原始值
//                    .setActualValue(Math.random() * 100);    // 随机生成实际值
//
//            // 添加到数据列表
//            monitorDataList.add(data);
//        }
//
//        return monitorDataList;
//    }
//
//    public static void readDataFromFileAndWrite(InfluxDBClient client, String filePath) {
//        List<MonitorData> monitorDataBatch = new ArrayList<>();
//        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                // 解析数据
//                MonitorData data = parseLineToMonitorData(line);
//                monitorDataBatch.add(data);
//
//                // 当批量数据达到指定大小时，写入 InfluxDB
//                if (monitorDataBatch.size() >= BATCH_SIZE) {
//                    writeData(client, monitorDataBatch);
//                    monitorDataBatch.clear();  // 清空批次数据
//                }
//            }
//
//            // 写入剩余的数据（不足一批的数据）
//            if (!monitorDataBatch.isEmpty()) {
//                writeData(client, monitorDataBatch);
//            }
//
//        } catch (IOException e) {
//            log.error("Error reading data from file: " + filePath, e);
//        }
//    }
//
//    public static MonitorData parseLineToMonitorData(String line) {
//        // 示例数据格式: sensor_data,decoder=1,channel=Ch1 value=1538.46 1720713600000000000
//        String[] parts = line.split(" ");  // 按空格分割
//        String sensorData = parts[0]; // "sensor_data,decoder=1,channel=Ch1"
//        String valueStr = parts[1]; // "value=1538.46"
//        String timestampStr = parts[2]; // "1720713600000000000" 时间戳
//
//        // 解析时间戳
//        long epochNano = Long.parseLong(timestampStr);
//        long epochSecond = epochNano / 1_000_000_000; // 获取秒部分
//        int nanoAdjustment = (int) (epochNano % 1_000_000_000); // 获取纳秒部分
//
//        Instant timestamp = Instant.ofEpochSecond(epochSecond, nanoAdjustment);
//
//        // 解析传感器数据部分
//        String[] sensorDataParts = sensorData.split(",");
//        String decoderId = sensorDataParts[1].split("=")[1]; // 解析 decoderId
//        String channelId = sensorDataParts[2].split("=")[1]; // 解析 channelId
//
//        // 使用 Double 解析原始值
//        String value = valueStr.split("=")[1]; // 提取 value=1538.46 后的数值部分
//        double originalValue = Double.parseDouble(value); // 解析为 double 类型
//
//        // 创建 MonitorData 对象并返回
//        return new MonitorData()
//                .setDecoderId(decoderId)
//                .setChannelId(channelId)
//                .setLocationTime(timestamp)
//                .setOriginalValue(originalValue)
//                .setActualValue(originalValue); // 实际值可以根据需求设定
//    }
//
//    public static void processAndWriteFile(String filePath, InfluxDBClient client) throws IOException {
//        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
//        String influxDbOrg = "test";
//        String influxDbBucket = "test8";
//        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
//            String line;
//            List<String> batch = new ArrayList<>();
//            int processedLines = 0;
//            int batchCount = 0;
//
//            long totalStartTime = System.nanoTime(); // 记录总处理时间
//
//            while ((line = reader.readLine()) != null) {
//                // 跳过非数据行
//                if (line.startsWith("W")) {
//                    continue;
//                }
//
//                // 解析数据行为Line Protocol格式
//                String lineProtocol = parseLineToMonitorData0(line);
//
//                // 添加到批处理列表
//                batch.add(lineProtocol);
//                processedLines++;
//
//                // 每达到 BATCH_SIZE，就写入一次数据库
//                if (batch.size() >= BATCH_SIZE) {
//                    long batchStartTime = System.nanoTime(); // 记录每批次写入开始时间
//                    writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batch));
//                    long batchEndTime = System.nanoTime(); // 记录每批次写入结束时间
//
//                    // 打印每批次写入时间
//                    long batchDuration = (batchEndTime - batchStartTime) / 1_000_000; // 毫秒
//                    System.out.println("Batch " + (batchCount + 1) + " 写入数据库: " + batch.size() + " 条数据, 花费时间: " + batchDuration + "ms");
//
//                    batch.clear(); // 清空批次
//                    batchCount++; // 增加批次计数
//
////                     如果已经处理了10000行数据，停止程序
//                    if (processedLines >= 3200000) {
//                        System.out.println("已处理 100000 行数据，停止程序。");
//                        break;
//                    }
//                }
//            }
//
//            // 如果文件处理完剩余的数据还没有写入，进行一次写入
//            if (!batch.isEmpty()) {
//                long batchStartTime = System.nanoTime(); // 记录剩余批次的写入开始时间
//                writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batch));
//                long batchEndTime = System.nanoTime(); // 记录剩余批次的写入结束时间
//
//                // 打印剩余批次写入时间
//                long batchDuration = (batchEndTime - batchStartTime) / 1_000_000; // 毫秒
//                System.out.println("最后 Batch 写入数据库: " + batch.size() + " 条数据, 花费时间: " + batchDuration + "ms");
//            }
//
//            long totalEndTime = System.nanoTime(); // 记录总处理时间结束
//            long totalDuration = (totalEndTime - totalStartTime) / 1_000_000; // 毫秒
//            System.out.println("总处理时间: " + totalDuration + "ms");
//        }
//    }
//
//    private static String parseLineToMonitorData0(String line) {
//        // 假设每行的数据格式为：sensor_data,decoder=1,channel=Ch1 value=1538.46 1720713600000000000
//        String[] parts = line.split("\\s+");
//        if (parts.length < 2) {
//            return null;
//        }
//
//        String[] channelParts = parts[0].split(",");
//        String channel = channelParts[2].split("=")[1];
//
//        String value = parts[1].split("=")[1];
//        String timestampStr = parts[2];
//
//        // 转换为Line Protocol格式
//        return String.format("sensor_data,decoder=1,channel=ch%s value=%s %s", channel, value, timestampStr);
//    }

    public static void processAndWriteFile1(InfluxDBClient client, String filePath, int maxRows) throws IOException {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        List<String> batchLines = new ArrayList<>(BATCH_SIZE);

        // 从文件路径提取解调器编号
        String decoderId = filePath.split("\\\\")[2];
        int decoderNumber = Integer.parseInt(decoderId);

        // 跳过表头
        reader.readLine();

        String line;
        Map<String, Integer> timeCounts = new HashMap<>();
        int processedLines = 0;

        while ((line = reader.readLine()) != null && processedLines < maxRows) {
            String[] columns = line.split("\t");
            if (columns.length < 4) continue;

            String baseTime = columns[2].trim();
            int counter = timeCounts.getOrDefault(baseTime, 0);
            if (counter >= 1000) continue;

            timeCounts.put(baseTime, counter + 1);
            long timestampNs = convertTimestamp(baseTime, counter, 1000);

            // 处理32个通道数据
            for (int channel = 0; channel < 32; channel++) {
                int columnIndex = 3 + channel * 3;
                if (columnIndex >= columns.length) break;

                String valueStr = columns[columnIndex].replace("|", "").trim();
                double value;
                try {
                    value = valueStr.isEmpty() ? 0.0 : Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    value = 0.0;
                }

                // 构建单通道行协议
                String lp = String.format(
                        "sensor_data,decoder=%d,channel=%d Ch_ori=%f %d",
                        decoderNumber,
                        channel + 1,
                        value,
                        timestampNs
                );

                batchLines.add(lp);

                // 批量写入控制
                if (batchLines.size() >= BATCH_SIZE) {
                    writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg,
                            WritePrecision.NS, String.join("\n", batchLines));
                    System.out.println("开始写入，当前processedLines： " + processedLines);
                    batchLines.clear();
                }
            }
            processedLines++;
        }

        // 写入剩余数据
        if (!batchLines.isEmpty()) {
            writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg,
                    WritePrecision.NS, String.join("\n", batchLines));
        }

        reader.close();
    }

    public static void writeDataFromFile0(InfluxDBClient client, String filePath, int maxRows) throws IOException {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        Map<String, Integer> timeCounts = new HashMap<>();
        int processedLines = 0;
        long startTime = System.currentTimeMillis();
        List<String> batchLines = new ArrayList<>(BATCH_SIZE);

        // 从文件路径中提取解调器编号
        String decoderId = filePath.split("\\\\")[2];
        int decoderNumber = Integer.parseInt(decoderId);

        // 跳过文件的第一行表头
        reader.readLine();

        // 读取文件并处理
        while ((line = reader.readLine()) != null && processedLines < maxRows) {
            String[] columns = line.split("\t");
            if (columns.length < 4) {
                continue;
            }

            String baseTime = columns[2].trim();
            int counter = timeCounts.getOrDefault(baseTime, 0);

            if (counter >= 1000) {
                continue;
            }

            timeCounts.put(baseTime, counter + 1);

            // 转换基准时间为纳秒时间戳
            long timestampNs = convertTimestamp(baseTime, counter, 1000);

            // 只提取原始值
            double[] originalValues = new double[32];
            for (int i = 3; i < columns.length && (i - 3) / 3 < 32; i += 3) {
                String value = columns[i].replace("|", "").trim();
                try {
                    originalValues[(i - 3) / 3] = value.isEmpty() ? 0.0 : Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    originalValues[(i - 3) / 3] = 0.0;
                }
            }

            // 构建行协议（只包含原始值）
            StringBuilder lineProtocolBuilder = new StringBuilder();
            lineProtocolBuilder.append(String.format("sensor_data,decoder=%d ", decoderNumber));
            for (int channel = 0; channel < 32; channel++) {
                lineProtocolBuilder.append(String.format("Ch%d_ori=%f,", channel + 1, originalValues[channel]));
            }
            lineProtocolBuilder.setLength(lineProtocolBuilder.length() - 1);
            lineProtocolBuilder.append(String.format(" %d", timestampNs));

            batchLines.add(lineProtocolBuilder.toString());
            processedLines++;

            if (batchLines.size() >= BATCH_SIZE) {
                writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS,
                        String.join("\n", batchLines));
                batchLines.clear();
            }
        }

        // 处理剩余的批量数据
        if (!batchLines.isEmpty()) {
            writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS,
                    String.join("\n", batchLines));
        }

        reader.close();

        // 统计结果输出
        long elapsedTime = System.currentTimeMillis() - startTime;
        double writeSpeed = processedLines / (elapsedTime / 1000.0);

        System.out.println("\n===== 写入统计 =====");
        System.out.println("文件路径: " + filePath);
        System.out.println("最大行数限制: " + maxRows);
        System.out.println("实际写入行数: " + processedLines);
        System.out.println("总耗时(ms): " + elapsedTime);
        System.out.printf("写入速度(条/s): %.2f%n", writeSpeed);
        System.out.println("===================");
    }

    public static void writeDataFromFile1(InfluxDBClient client, String filePath) throws IOException {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        Map<String, Integer> timeCounts = new HashMap<>(); // 记录每个时间点的计数
        int processedLines = 0;
        int batchCount = 0;
        long startTime = System.currentTimeMillis(); // 记录开始时间
        long t0 = 0;
        long t1 = 0;
        long t2 = 0;
        long t3 = 0;
        List<String> batchLines = new ArrayList<>(BATCH_SIZE);

        // 从文件路径中提取解调器编号
        String decoderId = filePath.split("\\\\")[2]; // 例如：从 "E:\\decoder\\01\\Wave_20240712_010000.txt" 中提取 "01"
        int decoderNumber = Integer.parseInt(decoderId); // 转换为整数

        // 跳过文件的第一行表头
        reader.readLine();

        // 读取文件并处理
        while ((line = reader.readLine()) != null) {
            long tn = System.currentTimeMillis();
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
            long tm = System.currentTimeMillis();
            t0 = t0 + tm - tn;
            // 计算实际值和修正值
            CalculateResult result = Calculator.calculate(originalValues, decoderNumber);
            double[] actualValues = result.getActualValues();
            Map<String, Double> reviseValues = result.getReviseValues();

            tn = System.currentTimeMillis();
            t1 = t1 + tn - tm;

            // 创建一个 StringBuilder 来构建包含所有信道数据的行协议
            StringBuilder lineProtocolBuilder = new StringBuilder();
            lineProtocolBuilder.append(String.format("sensor_data,decoder=%d ", decoderNumber)); // 设置解调器编号

            // 处理每个信道的原始值和实际值
            for (int channel = 0; channel < 32; channel++) {
                lineProtocolBuilder.append(String.format("Ch%d_ori=%f,", channel + 1, originalValues[channel])); // 原始值
                lineProtocolBuilder.append(String.format("Ch%d_act=%f,", channel + 1, actualValues[channel])); // 实际值
            }

            // 处理修正值
            for (Map.Entry<String, Double> entry : reviseValues.entrySet()) {
                lineProtocolBuilder.append(String.format("%s=%f,", entry.getKey(), entry.getValue()));
            }
            tm = System.currentTimeMillis();
            t2 = t2 + tm - tn;
            // 去掉最后一个逗号并添加时间戳
            if (lineProtocolBuilder.length() > 0) {
                lineProtocolBuilder.setLength(lineProtocolBuilder.length() - 1); // 去掉最后一个逗号
                lineProtocolBuilder.append(String.format(" %d", timestampNs));

                // 将行协议添加到批量中
                batchLines.add(lineProtocolBuilder.toString());
                processedLines++;

                // 如果批量达到指定大小，写入数据库
                if (batchLines.size() >= BATCH_SIZE) {
                    System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，读取拆分数据时间为：" + (t0 / 1000.0) + " 秒");
                    System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，计算实际值时间为：" + (t1 / 1000.0) + " 秒");
                    System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，拼接行协议时间为：" + (t2 / 1000.0) + " 秒");
                    tn = System.currentTimeMillis();
                    writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
                    batchLines.clear(); // 清空批量数据
                    t3 = t3 + System.currentTimeMillis() - tn;
                    batchCount++;
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，写入数据库时间为：" + (t3 / 1000.0) + " 秒");
                    System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，累计用时：" + (elapsedTime / 1000.0) + " 秒");
                }
            }
        }

        // 处理剩余的批量数据
        if (!batchLines.isEmpty()) {
            writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
            batchCount++;
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，累计用时：" + (elapsedTime / 1000.0) + " 秒");
        }

        reader.close();
        System.out.println("数据写入完成，共处理 " + processedLines + " 条数据。");
        return;
    }

    public static void writeDataFromFile2(InfluxDBClient client, String filePath) throws IOException {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        Map<String, Integer> timeCounts = new HashMap<>(); // 记录每个时间点的计数
        int processedLines = 0;
        int batchCount = 0;
        long startTime = System.currentTimeMillis(); // 记录开始时间
        long t0 = 0;
        long t1 = 0;
        long t2 = 0;
        long t3 = 0;
        List<Point> batchPoints = new ArrayList<>(BATCH_SIZE);

        // 提取解调器编号并转换为整数
        String decoderId = filePath.split("\\\\")[2];
        int decoderNumber = Integer.parseInt(decoderId);

        reader.readLine(); // 跳过表头

        while ((line = reader.readLine()) != null) {
            long tn = System.currentTimeMillis();
            String[] columns = line.split("\t");
            if (columns.length < 4) continue;

            String baseTime = columns[2].trim();
            int counter = timeCounts.getOrDefault(baseTime, 0);
            if (counter >= 1000) continue;
            timeCounts.put(baseTime, counter + 1);

            // 生成纳秒时间戳
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
            long tm = System.currentTimeMillis();
            t0 = t0 + tm - tn;

            // 计算实际值和修正值
            CalculateResult result = Calculator.calculate(originalValues, decoderNumber);
            double[] actualValues = result.getActualValues();
            Map<String, Double> reviseValues = result.getReviseValues();

            tn = System.currentTimeMillis();
            t1 = t1 + tn - tm;

            // 创建单条Point并添加所有信道数据
            Point point = Point.measurement("sensor_data")
                    .addTag("decoder", String.valueOf(decoderNumber)) // 标签统一用字符串类型
                    .time(timestampNs, WritePrecision.NS);

            // 添加32个信道的原始值和实际值
            for (int channel = 0; channel < 32; channel++) {
                String channelKey = "Ch" + (channel + 1);
                point.addField(channelKey + "_ori", originalValues[channel])
                        .addField(channelKey + "_act", actualValues[channel]);
            }

            // 添加修正值字段
            reviseValues.forEach((key, value) -> point.addField(key, value));

            tm = System.currentTimeMillis();
            t2 = t2 + tm - tn;

            batchPoints.add(point);
            processedLines++;

            // 批量写入
            if (batchPoints.size() >= BATCH_SIZE) {
                System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，读取拆分数据时间为：" + (t0 / 1000.0) + " 秒");
                System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，计算实际值时间为：" + (t1 / 1000.0) + " 秒");
                System.out.println("即将开始写入 " + batchCount * BATCH_SIZE + " 条数据，设置数据点时间为：" + (t2 / 1000.0) + " 秒");
                tn = System.currentTimeMillis();
                writeApiBlocking.writePoints(influxDbBucket, influxDbOrg, batchPoints);
                batchPoints.clear();
                t3 = t3 + System.currentTimeMillis() - tn;
                batchCount++;
                long elapsedTime = System.currentTimeMillis() - startTime;
                System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，写入数据库时间为：" + (t3 / 1000.0) + " 秒");
                System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，累计用时：" + (elapsedTime / 1000.0) + " 秒");
            }
        }

        // 处理剩余数据
        if (!batchPoints.isEmpty()) {
            writeApiBlocking.writePoints(influxDbBucket, influxDbOrg, batchPoints);
            batchCount++;
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，累计用时：" + (elapsedTime / 1000.0) + " 秒");
        }

        reader.close();
        System.out.println("数据写入完成，共处理 " + processedLines + " 条数据。");
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


    public static void testsearch(InfluxDBClient client,String channelId,String decoderId){
//        QueryApi queryApi = client.getQueryApi();
//        Long time1 = System.currentTimeMillis();
//        int pageSize = 1000;
//        int offset = 0;
//
//        while (true) {
//            // 定义 Flux 查询（带分页）
//        String fluxQuery = "from(bucket: \"test2\") " +
//                "|> range(start: 1720713600,stop:1720713605) " +
//                "|> filter(fn: (r) => r._measurement == \"sensor_data\") " +
//                "|> filter(fn: (r) => r.channel_id == \"1\" and r.decoder_id == \"01\") " +
//                "|> filter(fn: (r) => r.decoder_id == \"01\") " +
//                "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")" +
//                "|> limit(n: " + pageSize + ", offset: " + offset + ")";
//
//            // 查询数据
//            List<MonitorData> monitorDataList = queryApi.query(fluxQuery, MonitorData.class);
//
//            // 如果没有数据，退出循环
//            if (monitorDataList.isEmpty()) {
//                break;
//            }
//
//            // 打印当前页的数据
//            for (MonitorData data : monitorDataList) {
//                System.out.println(data);
//            }
//
//            // 更新 offset
//            offset += pageSize;
//        }
//        Long time2 = System.currentTimeMillis();
//        System.out.println((time2-time1)/1000.0);
//        String fluxQuery = "from(bucket: \"test2\") " +
//                "|> range(start: 1720713600,stop:1720713601) " +
//                "|> filter(fn: (r) => r._measurement == \"sensor_data\") " +
//                "|> filter(fn: (r) => r.channel_id == \"1\" and r.decoder_id == \"01\") " +
//                "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")" +
//                "|> limit(n: " + pageSize + ", offset: " + offset + ")";
//
//        System.out.println(fluxQuery);
//        Long time1 = System.currentTimeMillis();
//        List<MonitorData> monitorData = queryApi.query(fluxQuery, MonitorData.class);
//        Long time2 = System.currentTimeMillis();
//        System.out.println(monitorData.size());
//        System.out.println((time2-time1)/1000.0);
        return;
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

}
