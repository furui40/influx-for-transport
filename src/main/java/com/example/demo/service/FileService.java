package com.example.demo.service;

import com.example.demo.common.CommonResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileService {

    @Value("${data.file-dir}")
    private String fileDir;

    @Value("${data.base-dir:/data}")
    private String baseDir;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Shanghai"));

    // 保存文件
    public CommonResult<?> saveFiles(MultipartFile[] files) throws IOException {
        Path uploadPath = Paths.get(fileDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<String> savedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

            // 记录文件大小和上传时间(东八区)
            String fileMeta = filename + "|" + file.getSize() + "|" +
                    TIME_FORMATTER.format(ZonedDateTime.now());
            savedFiles.add(fileMeta);
        }

        // 更新索引文件
        updateIndexFile(savedFiles);

        return CommonResult.success(savedFiles.stream()
                .map(s -> s.split("\\|")[0])
                .collect(Collectors.toList()), "文件上传成功");
    }

    // 获取文件列表
    public CommonResult<List<String>> getFileList() throws IOException {
        Path indexPath = Paths.get(baseDir, "index.txt");
        if (!Files.exists(indexPath)) {
            return CommonResult.success(new ArrayList<>());
        }

        // 读取文件并按写入顺序逆序排列
        List<String> files = Files.readAllLines(indexPath);
        Collections.reverse(files);  //逆序排列

        return CommonResult.success(files);
    }
    // 加载文件资源
    public Resource loadFileAsResource(String filename) throws IOException {
        Path filePath = Paths.get(fileDir).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists()) {
            return resource;
        } else {
            throw new IOException("文件未找到: " + filename);
        }
    }

    // 删除文件
    public CommonResult<?> deleteFile(String filename) throws IOException {
        Path filePath = Paths.get(fileDir).resolve(filename);

        // 删除物理文件
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // 更新索引文件
        Path indexPath = Paths.get(baseDir, "index.txt");
        if (Files.exists(indexPath)) {
            List<String> remainingFiles = Files.readAllLines(indexPath)
                    .stream()
                    .filter(line -> !line.startsWith(filename + "|"))
                    .collect(Collectors.toList());

            Files.write(indexPath, remainingFiles,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }

        return CommonResult.success(null, "文件删除成功");
    }

    // 更新索引文件
    private void updateIndexFile(List<String> newFiles) throws IOException {
        Path indexPath = Paths.get(baseDir, "index.txt");
        List<String> existingFiles = new ArrayList<>();

        if (Files.exists(indexPath)) {
            existingFiles = Files.readAllLines(indexPath);
        }

        // 合并并去重(基于文件名)
        List<String> allFiles = new ArrayList<>(existingFiles);
        List<String> existingFileNames = existingFiles.stream()
                .map(line -> line.split("\\|")[0])
                .collect(Collectors.toList());

        allFiles.addAll(newFiles.stream()
                .filter(f -> !existingFileNames.contains(f.split("\\|")[0]))
                .collect(Collectors.toList()));

        // 写入索引文件
        Files.write(indexPath, allFiles,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
}