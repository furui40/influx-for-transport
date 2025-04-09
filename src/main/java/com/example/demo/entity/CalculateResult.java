package com.example.demo.entity;

import lombok.Data;

import java.util.Map;
import java.util.HashMap;

@Data
public class CalculateResult {
    private double[] actualValues; // 实际值数组
    private Map<String, Double> reviseValues; // 修正值键值对

    public CalculateResult(int channelCount) {
        this.actualValues = new double[channelCount];
        this.reviseValues = new HashMap<>();
    }
}