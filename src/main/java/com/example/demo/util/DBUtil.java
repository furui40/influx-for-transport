package com.example.demo.util;

import com.example.demo.entity.MonitorData;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.extern.slf4j.Slf4j;


import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.Duration;
import java.time.Instant;

@Slf4j
public class DBUtil {

    private static String influxDbOrg = "test";

    private static String influxDbBucket = "test2";

    public static final int BATCH_SIZE = 3200000;

    public static final int MAX_CHANNELS = 32; // 最大信道数量
    private static final int SAMPLING_FREQUENCY_HZ = 1000; // 采样频率 (Hz)
    // 写入测试数据

    public static void writeData(InfluxDBClient client, List<MonitorData> monitorData) {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();

        // 每次写入的数据量

        // 按照批次拆分数据并写入
        List<MonitorData> batch = new ArrayList<>();
        for (int i = 0; i < monitorData.size(); i++) {
            batch.add(monitorData.get(i));

            // 当批次大小达到设定值时，执行写入操作
            if (batch.size() >= BATCH_SIZE || i == monitorData.size() - 1) {
                // 写入当前批次的数据
                writeApiBlocking.writeMeasurements("test2", "test", WritePrecision.NS, batch);
                log.info("Batch {} written successfully, size: {}", i / BATCH_SIZE + 1, batch.size());

                // 清空批次
                batch.clear();
            }
        }

        log.info("All data written successfully to InfluxDB.");
    }


    public static List<MonitorData> generateTestData() {
        // 创建模拟的传感器数据
        List<MonitorData> monitorDataList = new ArrayList<>();

        // 获取当前时间作为第一个时间戳
        Instant time = Instant.now().minus(Duration.ofHours(2400));

        // 随机数生成器，用于生成时间间隔
        Random random = new Random();

        // 创建 5 条模拟数据
        for (int i = 0; i < 10000; i++) {
            // 随机生成1到1000毫秒之间的时间间隔
            long randomInterval = 1 + random.nextInt(10);

            // 设置时间戳为当前时间 + 随机时间间隔
            time = time.plusMillis(randomInterval);

            // 创建 MonitorData 对象并设置值
            MonitorData data = new MonitorData()
                    .setDecoderId("1")   // 设置解调器ID
                    .setChannelId("ch1")   // 设置信道ID
                    .setLocationTime(time)  // 设置时间，间隔随机
                    .setOriginalValue(Math.random() * 100)   // 随机生成原始值
                    .setActualValue(Math.random() * 100);    // 随机生成实际值

            // 添加到数据列表
            monitorDataList.add(data);
        }

        return monitorDataList;
    }

    public static void readDataFromFileAndWrite(InfluxDBClient client, String filePath) {
        List<MonitorData> monitorDataBatch = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = br.readLine()) != null) {
                // 解析数据
                MonitorData data = parseLineToMonitorData(line);
                monitorDataBatch.add(data);

                // 当批量数据达到指定大小时，写入 InfluxDB
                if (monitorDataBatch.size() >= BATCH_SIZE) {
                    writeData(client, monitorDataBatch);
                    monitorDataBatch.clear();  // 清空批次数据
                }
            }

            // 写入剩余的数据（不足一批的数据）
            if (!monitorDataBatch.isEmpty()) {
                writeData(client, monitorDataBatch);
            }

        } catch (IOException e) {
            log.error("Error reading data from file: " + filePath, e);
        }
    }

    public static MonitorData parseLineToMonitorData(String line) {
        // 示例数据格式: sensor_data,decoder=1,channel=Ch1 value=1538.46 1720713600000000000
        String[] parts = line.split(" ");  // 按空格分割
        String sensorData = parts[0]; // "sensor_data,decoder=1,channel=Ch1"
        String valueStr = parts[1]; // "value=1538.46"
        String timestampStr = parts[2]; // "1720713600000000000" 时间戳

        // 解析时间戳
        long epochNano = Long.parseLong(timestampStr);
        long epochSecond = epochNano / 1_000_000_000; // 获取秒部分
        int nanoAdjustment = (int) (epochNano % 1_000_000_000); // 获取纳秒部分

        Instant timestamp = Instant.ofEpochSecond(epochSecond, nanoAdjustment);

        // 解析传感器数据部分
        String[] sensorDataParts = sensorData.split(",");
        String decoderId = sensorDataParts[1].split("=")[1]; // 解析 decoderId
        String channelId = sensorDataParts[2].split("=")[1]; // 解析 channelId

        // 使用 Double 解析原始值
        String value = valueStr.split("=")[1]; // 提取 value=1538.46 后的数值部分
        double originalValue = Double.parseDouble(value); // 解析为 double 类型

        // 创建 MonitorData 对象并返回
        return new MonitorData()
                .setDecoderId(decoderId)
                .setChannelId(channelId)
                .setLocationTime(timestamp)
                .setOriginalValue(originalValue)
                .setActualValue(originalValue); // 实际值可以根据需求设定
    }

    public static void processAndWriteFile(String filePath, WriteApiBlocking writeApiBlocking) throws IOException {
        String influxDbOrg = "test";
        String influxDbBucket = "test2";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            List<String> batch = new ArrayList<>();
            int processedLines = 0;
            int batchCount = 0;

            long totalStartTime = System.nanoTime(); // 记录总处理时间

            while ((line = reader.readLine()) != null) {
                // 跳过非数据行
                if (line.startsWith("W")) {
                    continue;
                }

                // 解析数据行为Line Protocol格式
                String lineProtocol = parseLineToMonitorData0(line);

                // 添加到批处理列表
                batch.add(lineProtocol);
                processedLines++;

                // 每达到 BATCH_SIZE，就写入一次数据库
                if (batch.size() >= BATCH_SIZE) {
                    long batchStartTime = System.nanoTime(); // 记录每批次写入开始时间
                    writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batch));
                    long batchEndTime = System.nanoTime(); // 记录每批次写入结束时间

                    // 打印每批次写入时间
                    long batchDuration = (batchEndTime - batchStartTime) / 1_000_000; // 毫秒
                    System.out.println("Batch " + (batchCount + 1) + " 写入数据库: " + batch.size() + " 条数据, 花费时间: " + batchDuration + "ms");

                    batch.clear(); // 清空批次
                    batchCount++; // 增加批次计数

                    // 如果已经处理了10000行数据，停止程序
                    if (processedLines >= 10000) {
                        System.out.println("已处理 10000 行数据，停止程序。");
                        break;
                    }
                }
            }

            // 如果文件处理完剩余的数据还没有写入，进行一次写入
            if (!batch.isEmpty()) {
                long batchStartTime = System.nanoTime(); // 记录剩余批次的写入开始时间
                writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batch));
                long batchEndTime = System.nanoTime(); // 记录剩余批次的写入结束时间

                // 打印剩余批次写入时间
                long batchDuration = (batchEndTime - batchStartTime) / 1_000_000; // 毫秒
                System.out.println("最后 Batch 写入数据库: " + batch.size() + " 条数据, 花费时间: " + batchDuration + "ms");
            }

            long totalEndTime = System.nanoTime(); // 记录总处理时间结束
            long totalDuration = (totalEndTime - totalStartTime) / 1_000_000; // 毫秒
            System.out.println("总处理时间: " + totalDuration + "ms");
        }
    }

    private static String parseLineToMonitorData0(String line) {
        // 假设每行的数据格式为：sensor_data,decoder=1,channel=Ch1 value=1538.46 1720713600000000000
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            return null;
        }

        String[] channelParts = parts[0].split(",");
        String channel = channelParts[2].split("=")[1];

        String value = parts[1].split("=")[1];
        String timestampStr = parts[2];

        // 转换为Line Protocol格式
        return String.format("sensor_data,decoder=1,channel=ch%s value=%s %s", channel, value, timestampStr);
    }

    public static void writeDataFromFile(InfluxDBClient client, String filePath) throws IOException {
        WriteApiBlocking writeApiBlocking = client.getWriteApiBlocking();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        Map<String, Integer> timeCounts = new HashMap<>(); // 记录每个时间点的计数
        int processedLines = 0;
        int batchCount = 0;
        long startTime = System.currentTimeMillis(); // 记录开始时间
        List<String> batchLines = new ArrayList<>(BATCH_SIZE);
        List<String> batchLinesForFile = new ArrayList<>(BATCH_SIZE); // 用于写入文件的批量数据

        // 跳过文件的第一行表头
        reader.readLine();

        // 读取文件并处理
        while ((line = reader.readLine()) != null) {

            String[] columns = line.split("\t");
            if (columns.length < 4) {
                continue;
            }

            String baseTime = columns[2].trim(); // 基准时间
            int counter = timeCounts.getOrDefault(baseTime, 0); // 当前时间点的计数
            timeCounts.put(baseTime, counter + 1); // 更新计数器

            // 转换基准时间为纳秒时间戳
            long timestampNs = convertTimestamp(baseTime, counter, SAMPLING_FREQUENCY_HZ);

            // 处理每个信道的数据
            int channelIdx = 1;  // 从第一个信道开始
            for (int i = 3; i < columns.length; i++) {  // 从第4列开始
                String value = columns[i].replace("|", "").trim();

                if (value.isEmpty()) {
                    continue;  // 如果值为空或仅含竖线，则跳过
                }

                try {
                    float floatValue = Float.parseFloat(value);
                    if (channelIdx > MAX_CHANNELS) {
                        break; // 超过最大信道数，跳出
                    }

                    // 创建 InfluxDB 行协议
                    String lineProtocol = String.format(
                            "sensor_data,decoder=1,channel=Ch%d value=%f %d",
                            channelIdx, floatValue, timestampNs);

                    batchLines.add(lineProtocol); // 将行协议添加到批量中
                    batchLinesForFile.add(lineProtocol); // 将行协议添加到写入文件的批量中

                    processedLines++;

                    // 如果批量达到指定大小，写入文件和数据库
                    if (batchLines.size() >= BATCH_SIZE) {
                        // 写入文件
//                        writeBatchToFile(batchLinesForFile, filePath);

                        // 写入数据库
                        writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));

                        batchLines.clear(); // 清空批量数据
                        batchLinesForFile.clear(); // 清空写入文件的批量数据

                        batchCount++;
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        System.out.println("已写入 " + batchCount * BATCH_SIZE / 32 + " 条数据，累计用时：" + (elapsedTime / 1000.0) + " 秒");

                        // 如果处理了 10000000 条数据，停止程序
                        if (processedLines >= 10000000) {
                            System.out.println("已处理 10000000 条数据，程序中止！");
                            reader.close();
                            return; // 结束处理
                        }
                    }

                    // 只有在成功处理了有效值后，才增加 channelIdx
                    channelIdx++;

                } catch (NumberFormatException e) {
                    // 如果数据无法解析为数字，则跳过
                    continue;
                }
            }
        }

        // 处理剩余的批量数据
        if (!batchLines.isEmpty()) {
            // 写入文件
//            writeBatchToFile(batchLinesForFile, filePath);

            // 写入数据库
            writeApiBlocking.writeRecord(influxDbBucket, influxDbOrg, WritePrecision.NS, String.join("\n", batchLines));
            batchCount++;
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("已写入 " + batchCount * BATCH_SIZE + " 条数据，累计用时：" + (elapsedTime / 1000.0) + " 秒");
        }

        reader.close();
        System.out.println("数据写入完成，共处理 " + processedLines/32 + " 条数据。");
        return ;
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

    private static void writeBatchToFile(List<String> batchLines, String filePath) throws IOException {
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

    // 查询数据
    public static void queryData(InfluxDBClient client) {
        String flux = "from(bucket: \"test3\") "  // 这里双引号需要转义
                + "  |> range(start: 0) ";  // 查询时间范围
        // 执行查询
        List<FluxTable> query = client.getQueryApi().query(flux);

        // 输出查询结果
        for (FluxTable table : query) {
            List<FluxRecord> records = table.getRecords();
            for (FluxRecord record : records) {
                System.out.println("Measurement: " + record.getMeasurement() +
                        ", Field: " + record.getField() +
                        ", Value: " + record.getValue() +
                        ", Time: " + record.getTime().plus(Duration.ofHours(8)));
            }
        }
    }

}
