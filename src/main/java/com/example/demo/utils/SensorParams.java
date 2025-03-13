package com.example.demo.utils;

/**
 * 传感器参数类
 */
public class SensorParams {
    private double lambda0; // λ0
    private double K; // K
    private double a; // a
    private double b; // b
    private int method; // 计算方式（Ti, P, Epsilon）

    // 构造函数
    public SensorParams(double lambda0, double K, double a, double b, int method) {
        this.lambda0 = lambda0;
        this.K = K;
        this.a = a;
        this.b = b;
        this.method = method;
    }

    // Getter 方法
    public double getLambda0() {
        return lambda0;
    }

    public double getK() {
        return K;
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public int getMethod() {
        return method;
    }
}