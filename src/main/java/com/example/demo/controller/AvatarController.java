package com.example.demo.controller;

import com.example.demo.common.CommonResult;
import com.example.demo.common.ResultCode;
import com.example.demo.config.AvatarConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@RestController
@RequestMapping("/avatar")
public class AvatarController {

    private final AvatarConfig avatarConfig;

    public AvatarController(AvatarConfig avatarConfig) {
        this.avatarConfig = avatarConfig;
    }

    @PostMapping("/upload")
    public String uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam String userId) {
        try {
            // 确保路径是绝对路径
            Path uploadPath = Paths.get(avatarConfig.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath); // 创建目录（如果不存在）

            // 生成文件名
            String fileName = userId + ".png";
            Path filePath = uploadPath.resolve(fileName);

            // 保存文件
            file.transferTo(filePath.toFile());

            return "头像上传成功";
        } catch (IOException e) {
            e.printStackTrace();
            return "头像上传失败: " + e.getMessage();
        }
    }

    @GetMapping("/getavatar")
    public String getAvatar(@RequestParam String userId) throws IOException {
        Path path = Paths.get(avatarConfig.getUploadDir(), userId + ".png");

        if (!Files.exists(path)) {
            path = Paths.get(avatarConfig.getUploadDir(), "0.png");
            if (!Files.exists(path)) {
                return "默认头像不存在";
            }
        }

        byte[] fileContent = Files.readAllBytes(path);
        String base64Image = Base64.getEncoder().encodeToString(fileContent);
        return "data:image/png;base64," + base64Image;
    }
}