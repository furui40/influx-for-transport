package com.example.demo.utils;
import com.example.demo.common.ItemMapping;
import com.example.demo.entity.CalculateResult;
import com.example.demo.entity.SensorParams;

import java.util.List;
import java.util.Map;

public class Calculator {

    /**
     * 计算实际值
     *
     * @param originalValues 32 个信道的原始值
     * @param decoderNumber  解调器编号
     * @return 32 个信道的实际值
     */
    public static CalculateResult calculate(double[] originalValues, int decoderNumber) {
        CalculateResult result = new CalculateResult(32);

        // 根据解调器编号和信道编号选择计算方式
        for (int channel = 0; channel < 32; channel++) {
            int channelNumber = channel + 1;
            double lambda = originalValues[channel];

            String sensorKey = String.format("%02d_Ch%d", decoderNumber, channelNumber);
            SensorParams params = ItemMapping.sensorParamsMap.get(sensorKey);

            if (params == null) {
                params = new SensorParams(0.0, 1.0, 1.0, 1.0, 0);
            }

            // 先设置默认实际值
            result.getActualValues()[channel] = lambda;

            // 根据方法类型计算
            switch (params.getMethod()) {
                case 1:
                    result.getActualValues()[channel] = calculateTi(lambda, params);
                    break;
                case 2:
                    result.getActualValues()[channel] = calculateP2(lambda, originalValues, params);
                    break;
                case 3:
                    result.getActualValues()[channel] = calculateP3(lambda, originalValues, params);
                    break;
                case 4:
                    result.getActualValues()[channel] = calculateP4(lambda, originalValues, params);
                    break;
                case 5:
                    result.getActualValues()[channel] = calculateP5(lambda, originalValues, params);
                    break;
                case 6:
                    result.getActualValues()[channel] = calculateP6(lambda, originalValues, params);
                    break;
                case 7:
                    result.getActualValues()[channel] = calculateP7(lambda, originalValues, params);
                    break;
                case 8:
                    // 方法8只计算修正值，实际值保持原始值
                    calculateP8(channelNumber, decoderNumber, lambda, params, result.getReviseValues());
                    break;
                default:
                    break;
            }
        }

        return result;
    }

    /**
     * 计算 T = (λ - λ) / K
     *
     * @param lambda 当前信道的原始值
     * @param params 传感器参数
     * @return 计算结果
     */
    private static double calculateTi(double lambda, SensorParams params) {
        return (lambda - params.getLambda0()) / params.getK();
    }

    /**
     * 计算 P = [λ - λ0 - (λt - λ't) × b / a] / K
     *
     * @param lambda         当前信道的原始值
     * @param originalValues 所有信道的原始值
     * @param params         传感器参数
     * @return 计算结果
     */
    private static double calculateP2(double lambda, double[] originalValues, SensorParams params) {
        // 假设 λt 是信道 1 的原始值，λ't 是信道 2 的原始值
        double lambdaT = originalValues[9]; // λt
        double lambdaTPrime = 1540.044; // λ't

        return (lambda - params.getLambda0() - (lambdaT - lambdaTPrime) * params.getB() / params.getA()) / params.getK();
    }

    private static double calculateP3(double lambda, double[] originalValues, SensorParams params) {
        // 假设 λt 是信道 1 的原始值，λ't 是信道 2 的原始值
        double lambdaT = originalValues[20]; // λt
        double lambdaTPrime = 1550.852; // λ't

        return (lambda - params.getLambda0() - (lambdaT - lambdaTPrime) * params.getB() / params.getA()) / params.getK();
    }
    private static double calculateP4(double lambda, double[] originalValues, SensorParams params) {
        // 假设 λt 是信道 1 的原始值，λ't 是信道 2 的原始值
        double lambdaT = originalValues[19]; // λt
        double lambdaTPrime = 1553.875; // λ't

        return (lambda - params.getLambda0() - (lambdaT - lambdaTPrime) * params.getB() / params.getA()) / params.getK();
    }
    private static double calculateP5(double lambda, double[] originalValues, SensorParams params) {
        // 假设 λt 是信道 1 的原始值，λ't 是信道 2 的原始值
        double lambdaT = originalValues[9]; // λt
        double lambdaTPrime = 1549.809; // λ't

        return (lambda - params.getLambda0() - (lambdaT - lambdaTPrime) * params.getB() / params.getA()) / params.getK();
    }
    private static double calculateP6(double lambda, double[] originalValues, SensorParams params) {
        // 假设 λt 是信道 1 的原始值，λ't 是信道 2 的原始值
        double lambdaT = originalValues[20]; // λt
        double lambdaTPrime = 1556.838; // λ't

        return (lambda - params.getLambda0() - (lambdaT - lambdaTPrime) * params.getB() / params.getA()) / params.getK();
    }
    private static double calculateP7(double lambda, double[] originalValues, SensorParams params) {
        // 假设 λt 是信道 1 的原始值，λ't 是信道 2 的原始值
        double lambdaT = originalValues[19]; // λt
        double lambdaTPrime = 1559.858; // λ't

        return (lambda - params.getLambda0() - (lambdaT - lambdaTPrime) * params.getB() / params.getA()) / params.getK();
    }

    private static void calculateP8(int channelNumber, int decoderNumber,
                                    double lambda, SensorParams params,
                                    Map<String, Double> reviseValues) {
        List<Double> additionalParams = params.getAdditionalParams();
        if (additionalParams == null || additionalParams.isEmpty()) return;

        for (int i = 0; i < additionalParams.size(); i++) {
            double lambdaI = additionalParams.get(i);
            double pi = (lambda - lambdaI) / params.getK();

            String reviseKey = String.format("Ch%d_rev%d", channelNumber, i+1);
            reviseValues.put(reviseKey, pi);
        }
    }
}