package com.example.demo.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtil {

    private static final String LOG_FILE_PATH = "user_operations.log"; // 日志文件路径
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录用户操作日志
     *
     * @param userId      用户 ID
     * @param operation   操作类型（如 LOGIN, REGISTER, QUERY）
     * @param details     操作详情（如查询内容）
     */
    public static void logOperation(String userId, String operation, String details) {
        String logEntry = String.format(
                "[%s] User ID: %s, Operation: %s, Details: %s\n",
                LocalDateTime.now().format(DATE_FORMATTER), userId, operation, details
        );

        try (FileWriter writer = new FileWriter(LOG_FILE_PATH, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    public static void logOperation(String operation, String details) {
        String logEntry = String.format(
                "[%s], Operation: %s, Details: %s\n",
                LocalDateTime.now().format(DATE_FORMATTER), operation, details
        );

        try (FileWriter writer = new FileWriter(LOG_FILE_PATH, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }
}