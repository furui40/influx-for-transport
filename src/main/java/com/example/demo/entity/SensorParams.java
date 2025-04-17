package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 传感器参数类
 */
@Data
public class SensorParams {
    private double lambda0;
    private double K;
    private double a;
    private double b;
    private int method;
    private List<Double> additionalParams;

    // 兼容现有代码的构造函数，初始化additionalParams为空列表
    public SensorParams(double lambda0, double K, double a, double b, int method) {
        this(lambda0, K, a, b, method, new ArrayList<>());
    }

    // 新构造函数，允许传入additionalParams
    public SensorParams(double lambda0, double K, double a, double b, int method, List<Double> additionalParams) {
        this.lambda0 = lambda0;
        this.K = K;
        this.a = a;
        this.b = b;
        this.method = method;
        this.additionalParams = (additionalParams != null) ? new ArrayList<>(additionalParams) : new ArrayList<>();
    }
}