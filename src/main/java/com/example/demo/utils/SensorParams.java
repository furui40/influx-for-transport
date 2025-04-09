package com.example.demo.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 传感器参数类
 */
@Data
@AllArgsConstructor
public class SensorParams {
    private double lambda0; // λ0
    private double K; // K
    private double a; // a
    private double b; // b
    private int method; // 计算方式（Ti, P, Epsilon）

}