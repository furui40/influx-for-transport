package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.FileStatus;
import com.example.demo.service.DataManageService;
import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
@RequestMapping("/update")
public class UpdateController {

    private final InfluxDBClient influxDBClient;
    private final DataManageService dataManageService;

    @GetMapping("/status")
    public CommonResult<List<FileStatus>> getFileStatus(@RequestParam String dataType) {
        try {
            List<FileStatus> statusList = dataManageService.getOrCreateStatus(dataType);
            System.out.println("datatype:" + dataType);
            return CommonResult.success(statusList);
        } catch (IOException e) {
            return CommonResult.failed("状态文件操作失败：" + e.getMessage());
        }
    }

    @PostMapping("/process")
    public CommonResult<String> processFiles(
            @RequestParam String dataType,
            @RequestParam String filePaths) {
        try {
            System.out.println("dataType: " + dataType);
            System.out.println("filePaths: " + filePaths);
            // 解码文件路径
            List<String> filePathList = Arrays.asList(filePaths.split(","));

            dataManageService.processFiles(influxDBClient, dataType, filePathList);
            return CommonResult.success("文件处理完成");
        } catch (Exception e) {
            return CommonResult.failed("文件处理失败：" + e.getMessage());
        }
    }
}