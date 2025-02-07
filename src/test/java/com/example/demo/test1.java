package com.example.demo;

import com.example.demo.entity.DateTime;
import com.example.demo.entity.MonitorData;
import com.example.demo.util.DBUtil;
import com.example.demo.util.DateUtil;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class test1 {

    @Mock
    private InfluxDBClient influxDBClient; // 模拟 InfluxDBClient

    @Mock
    private WriteApi writeApi; // 模拟 WriteApi

    @InjectMocks
    private DBUtil dbUtil; // 被测试的 DBUtil 类

    @BeforeEach
    public void setUp() {
        // 配置模拟的 InfluxDBClient 以返回 mock 的 WriteApi
        when(influxDBClient.getWriteApi()).thenReturn(writeApi);
    }

    @Test
    public void testSaveMonitorData2InfluxDB() {
        // 创建一个测试的 MonitorData 对象
        MonitorData monitorData = new MonitorData();
        monitorData.setMonitorDeviceCode("device123");
        monitorData.setValue("10.5");
        monitorData.setTime("2024-07-28T12:30:00.000Z");

        // 调用 saveMonitorData2InfluxDB 方法
        dbUtil.saveMonitorData2InfluxDB(monitorData);


    }

}
