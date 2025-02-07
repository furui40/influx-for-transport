package com.example.demo.util;

import com.beust.jcommander.internal.Lists;
import com.example.demo.entity.DateTime;
import com.example.demo.entity.MonitorData;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DBUtil {

    @Resource
    private InfluxDBClient influxDBClient;

    @Value("${spring.influx.org}")
    private String organization;//这个属性不能命名为 org 因为和@Slf4j 冲突，会报错

    @Value("${spring.influx.bucket}")
    private String bucket;

    private static final String MEASUREMENT = "monitor_data";

    public void writePoints(String bucket, String org, List<Point> points) {
        if (CollUtil.isEmpty(points)) {
//            log.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= writePoints 数据为空，不写入！");
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= writePoints 数据为空，不写入！");
            return;
        }
        WriteOptions writeOptions = WriteOptions.builder()
                .batchSize(5000)
                .flushInterval(1000)
                .bufferLimit(10000)
                .jitterInterval(0)
                .retryInterval(5000)
                .build();
        try (WriteApi writeApi = influxDBClient.getWriteApi(writeOptions)) {
//            log.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxDB 将写入 {} 个点位", points.size());
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxDB 将写入" + points.size() +"个点位");
            writeApi.writePoints(bucket, org, points);
        } catch (Exception e) {
//            log.error("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxdb 写入失败！", e);
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxdb 写入失败！" + e);
        }
    }

    public void writePointsBlocking(String bucket, String org, List<Point> points) {
        if (CollUtil.isEmpty(points)) {
//            log.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= writePointsBlocking 数据为空，不写入！");
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= writePointsBlocking 数据为空，不写入！");
            return;
        }
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
//            log.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxDB 将阻塞写入 {} 个点位", points.size());
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxDB 将阻塞写入 " + points.size() + " 个点位");
            writeApi.writePoints(bucket, org, points);
//            log.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxDB 阻塞写入成功 {} 个点位！", points.size());
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxDB 阻塞写入成功 " + points.size() + " 个点位！");
        } catch (Exception e) {
//            log.error("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxdb 阻塞写入失败！", e);
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-= influxdb 阻塞写入失败！");
        }
    }

    public List<FluxTable> query(String statement) {
        return influxDBClient.getQueryApi().query(statement);
    }

    public void delete(OffsetDateTime start, OffsetDateTime stop, String predicate, String bucket, String org) {
        influxDBClient.getDeleteApi().delete(start, stop, predicate, bucket, org);
    }


    public List<MonitorData> queryInfluxdb(MonitorData monitorData,
                                           DateTime begin,
                                           DateTime end) {
        String query = buildQuery(begin, end, monitorData);
        TimeInterval timer = new TimeInterval();
        List<FluxTable> table = query(query);
//        log.info("******************** influxDBUtil.查询 执行完成，耗时{} ********************", DateUtil.formatBetween(timer.interval()));
        System.out.println("******************** influxDBUtil.查询 执行完成，耗时" + DateUtil.formatBetween(timer.interval()) + " ********************");
        timer.restart();
        List<MonitorData> monitorDataList = Lists.newArrayList();
        for (FluxTable fluxTable : table) {
            List<FluxRecord> fluxRecords = fluxTable.getRecords();
            for (FluxRecord record : fluxRecords) {
                Map<String, Object> values = record.getValues();
                MonitorData monitorData1 = new MonitorData();
                monitorData1.setMonitorDeviceCode(MapUtil.getStr(values, "monitor_device_code"));
                monitorData1.setValue(MapUtil.getStr(values, "_value"));
                monitorData1.setTime(DateUtil.toLocalDateTime(MapUtil.getDate(values, "_time")));
                monitorDataList.add(monitorData1);
            }
        }
//        log.info("******************** result size:" + monitorDataList.size() + " 遍历influx数据转换完成，耗时{} ********************", DateUtil.formatBetween(timer.interval()));
        System.out.println("******************** result size:" + monitorDataList.size() + " 遍历influx数据转换完成，耗时" + DateUtil.formatBetween(timer.interval()) + " ********************");
        return monitorDataList;

    }

    /**
     * 构建普通查询
     *
     * @param begin
     * @param end
     * @param monitorData
     * @return
     */
    private String buildQuery(DateTime begin, DateTime end, MonitorData monitorData) {
        String query = buildQueryStr(begin, end, monitorData);
        query += " |> sort(columns:[\"_time\"], desc:false)";
        return query;
    }

    /**
     * 构建分页查询
     *
     * @param begin
     * @param end
     * @param monitorData
     * @return
     */
    public String[] buildQuery4page(DateTime begin, DateTime end, MonitorData monitorData, Long pageSize, Long pageNum) {
        String query = buildQueryStr(begin, end, monitorData);
        String count = query + "|> group()" + " |> count()";
        query += " |> sort(columns:[\"_time\"], desc:true)";
        query += " |> group()";
        query += " |> limit(n: " + pageSize + ", offset: " + (--pageNum * pageSize) + ")";
        return new String[]{query, count};
    }

    private String buildQueryStr(DateTime begin, DateTime end, MonitorData monitorData) {
        DateTime beginFormatDate = DateUtil.offsetHour(begin, -8);
        DateTime endFormatDate = DateUtil.offsetHour(end, -8);
        String query = "from(bucket: \"" + bucket + "\") |> range(start: " + DateUtil.format(beginFormatDate, DatePattern.UTC_PATTERN)
                + ", stop: " + DateUtil.format(DateUtil.offsetSecond(endFormatDate, 1), DatePattern.UTC_PATTERN) +
                ") |> filter(fn: (r) => r[\"_measurement\"] == \"" + MEASUREMENT + "\"" + ")";
        String filter = "";
        String monitorDeviceCode = monitorData.getMonitorDeviceCode();
        if (StringUtils.isNotBlank(monitorDeviceCode)) {
            List<String> collect = Arrays.stream(monitorDeviceCode.split(",")).collect(Collectors.toList());
            String filter1 = " |> filter(fn: (r) => " + collect.stream().map(u -> " r[\"monitor_device_code\"] == \"" + u + "\"").collect(Collectors.joining("or", "", ")"));
            filter += filter1;
        }
        String filterValue = "|> filter(fn: (r) => r[\"_field\"] == \"value\")";
        filter += filterValue;
        query += filter;
        return query;
    }

//    private DateTime formatDate(Date collectTime) {
//        String formatStartDateStr = DateUtil.formatDate(collectTime);
//        return DateUtil.parse(formatStartDateStr + " " + String.format("%02d", DateUtil.hour(collectTime, true)) + ":00:00");
//    }


    public void saveMonitorData2InfluxDB(MonitorData monitorData) {
        List<Point> pointList = new ArrayList<>();
        Point point = Point
                .measurement(MEASUREMENT)
                .time(DateUtil.date(monitorData.getTime()).toInstant(), WritePrecision.NS)
                .addTag("monitorDeviceCode", monitorData.getMonitorDeviceCode())
                .addField("value", monitorData.getValue());
        pointList.add(point);
        writePoints(bucket, organization, pointList);
    }

}

