package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.entity.FileStatus;
import com.example.demo.entity.UploadRecord;
import com.example.demo.service.DataManageService;
import com.example.demo.service.UploadRecordService;
import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/upload")
public class UploadController {

    private final InfluxDBClient influxDBClient;
    private final DataManageService dataManageService;
    private final UploadRecordService uploadRecordService;

    @GetMapping("/status")
    public CommonResult<List<FileStatus>> getFileStatus(@RequestParam String dataType) {
        try {
            List<FileStatus> statusList = dataManageService.getOrCreateStatus(dataType);
//            for(FileStatus status: statusList){
//                System.out.println(status);
//            }
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

            filePathList.forEach(filePath ->
                    uploadRecordService.saveRecord(dataType, filePath, "processing"));
            dataManageService.processFiles(influxDBClient, dataType, filePathList);

            filePathList.forEach(filePath ->
                    uploadRecordService.saveRecord(dataType, filePath, "processed"));
            return CommonResult.success("文件处理完成");
        } catch (Exception e) {
            return CommonResult.failed("文件处理失败：" + e.getMessage());
        }
    }

    @GetMapping("/get_history")
    public CommonResult<List<UploadRecord>> getHistory(@RequestParam String dataType) {
        try {
            System.out.println(uploadRecordService.getRecords(dataType));
            return CommonResult.success(uploadRecordService.getRecords(dataType));
        } catch (Exception e) {
            return CommonResult.failed("获取历史失败：" + e.getMessage());
        }
    }

    @PostMapping("/delete_history")
    public CommonResult<String> deleteHistory(
            @RequestParam String dataType,
            @RequestParam String filePath) {
        try {
            uploadRecordService.deleteRecord(dataType, filePath);
            return CommonResult.success("删除成功");
        } catch (Exception e) {
            return CommonResult.failed("删除失败：" + e.getMessage());
        }
    }
}