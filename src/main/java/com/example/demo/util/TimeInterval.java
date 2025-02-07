package com.example.demo.util;

public class TimeInterval {

    private long startTime;

    // 构造方法
    public TimeInterval() {
        restart();
    }

    // 重新启动计时器
    public void restart() {
        startTime = System.nanoTime();
    }

    // 获取当前时间间隔（单位：秒）
    public long interval() {
        return (System.nanoTime() - startTime) / 1_000_000_000;  // 转换为秒
    }

    // 获取当前时间间隔（单位：毫秒）
    public long intervalMillis() {
        return (System.nanoTime() - startTime) / 1_000_000;  // 转换为毫秒
    }

    // 获取当前时间间隔（单位：微秒）
    public long intervalMicros() {
        return (System.nanoTime() - startTime) / 1_000;  // 转换为微秒
    }
}
