package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadRecord {
    private String dataType;
    private String filePath;
    private String status;
    private Long timestamp;
}
