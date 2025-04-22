package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.service.FileService;
import com.example.demo.utils.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    @Value("${data.base-dir:/data}")
    private String baseDir;

    // 上传文件
    @PostMapping("/upload")
    public CommonResult<?> uploadFiles(@RequestParam("files") MultipartFile[] files,
                                       @RequestHeader("userId") String userId) {
        try {
            String fileNames = String.join(", ",
                    java.util.Arrays.stream(files)
                            .map(MultipartFile::getOriginalFilename)
                            .toArray(String[]::new));

            LogUtil.logOperation(userId, "UPLOAD",
                    "Files: " + fileNames + ", Total size: " +
                            java.util.Arrays.stream(files)
                                    .mapToLong(MultipartFile::getSize)
                                    .sum() + " bytes");

            CommonResult<?> result = fileService.saveFiles(files);
            return result;
        } catch (IOException e) {
            LogUtil.logOperation(userId, "UPLOAD_ERROR", e.getMessage());
            return CommonResult.failed("文件上传失败: " + e.getMessage());
        }
    }

    // 获取文件列表
    @GetMapping("/list")
    public CommonResult<List<String>> listFiles(@RequestHeader("userId") String userId) {
        try {
            LogUtil.logOperation(userId, "LIST_FILES", "Request file list");
            return fileService.getFileList();
        } catch (IOException e) {
            LogUtil.logOperation(userId, "LIST_FILES_ERROR", e.getMessage());
            return CommonResult.failed("获取文件列表失败: " + e.getMessage());
        }
    }

    // 下载文件
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename,
                                                 @RequestHeader("userId") String userId) throws IOException {
        try {
            LogUtil.logOperation(userId, "DOWNLOAD", "File: " + filename);
            Resource resource = fileService.loadFileAsResource(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            LogUtil.logOperation(userId, "DOWNLOAD_ERROR", "File: " + filename + ", Error: " + e.getMessage());
            throw e;
        }
    }

    // 删除文件
    @DeleteMapping("/delete")
    public CommonResult<?> deleteFile(@RequestParam String filename,
                                      @RequestHeader("userId") String userId) {
        try {
            LogUtil.logOperation(userId, "DELETE", "File: " + filename);
            return fileService.deleteFile(filename);
        } catch (IOException e) {
            LogUtil.logOperation(userId, "DELETE_ERROR", "File: " + filename + ", Error: " + e.getMessage());
            return CommonResult.failed("文件删除失败: " + e.getMessage());
        }
    }
}