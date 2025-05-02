package com.example.demo.utils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SafeFileProcessor {
    /**
     * 线程安全地读取文件块并找到正确的起始位置
     * @param filePath 文件路径
     * @param startByte 起始字节
     * @param endByte 结束字节
     * @return 有效数据行列表（保证从完整秒开始）
     */
    public static List<String> readFileChunk(String filePath, long startByte, long endByte) throws IOException {
        List<String> lines = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(startByte);

            // 跳过第一行（可能不完整）
            if (startByte != 0) raf.readLine();

            // 寻找下一个完整秒的起始行
            String line;
            long currentSecond = -1;
            while (raf.getFilePointer() < endByte && (line = raf.readLine()) != null) {
                String[] cols = line.split("\t");
                if (cols.length < 4) continue;

                // 解析当前行秒级时间戳
                long secTimestamp = parseSecondTimestamp(cols[2]);
                if (currentSecond == -1) currentSecond = secTimestamp;

                // 如果秒变化，重置计数器
                if (secTimestamp != currentSecond) {
                    lines.clear();
                    currentSecond = secTimestamp;
                }
                lines.add(line);
            }
        }
        return lines;
    }

    public static long parseSecondTimestamp(String timeStr) {
        // 示例时间格式：2024-07-12,00:07:30
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd,HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(timeStr, formatter);
        return dateTime.toEpochSecond(ZoneOffset.UTC);
    }
}