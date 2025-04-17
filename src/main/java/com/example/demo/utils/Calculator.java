package com.example.demo.utils;
import com.example.demo.common.ItemMapping;
import com.example.demo.entity.CalculateResult;
import com.example.demo.entity.SensorParams;

import java.util.List;
import java.util.Map;

public class Calculator {

    public static CalculateResult calculate(double[] originalValues, int decoderNumber) {
        CalculateResult result = new CalculateResult(32);

        for (int channel = 0; channel < 32; channel++) {
            int channelNumber = channel + 1;
            double lambda = originalValues[channel];

            String sensorKey = String.format("%02d_Ch%d", decoderNumber, channelNumber);
            SensorParams params = ItemMapping.sensorParamsMap.getOrDefault(
                    sensorKey,
                    new SensorParams(0.0, 1.0, 1.0, 1.0, 0)
            );

            result.getActualValues()[channel] = lambda; // 默认值

            switch (params.getMethod()) {
                case 1:
                    result.getActualValues()[channel] = calculateT(lambda, params);
                    break;
                case 2:
                    result.getActualValues()[channel] = calculateP(lambda, params, originalValues[9], 1540.044);
                    break;
                case 3:
                    result.getActualValues()[channel] = calculateP(lambda, params, originalValues[20], 1550.852);
                    break;
                case 4:
                    result.getActualValues()[channel] = calculateP(lambda, params, originalValues[19], 1553.875);
                    break;
                case 5:
                    result.getActualValues()[channel] = calculateP(lambda, params, originalValues[9], 1549.809);
                    break;
                case 6:
                    result.getActualValues()[channel] = calculateP(lambda, params, originalValues[20], 1556.838);
                    break;
                case 7:
                    result.getActualValues()[channel] = calculateP(lambda, params, originalValues[19], 1559.858);
                    break;
                case 8:
                    calculateZYB(channelNumber, decoderNumber, lambda, params, result.getReviseValues());
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    /**
     * 通用 P 值计算方法（优化后）
     * @param lambda       当前信道原始值
     * @param params       传感器参数
     * @param lambdaT      参考信道原始值
     * @param lambdaTPrime 参考波长固定值
     */
    private static double calculateP(double lambda,
                                     SensorParams params,
                                     double lambdaT,
                                     double lambdaTPrime) {
        return (lambda - params.getLambda0() - (lambdaT - lambdaTPrime) * params.getB() / params.getA()) / params.getK();
    }

    private static double calculateT(double lambda, SensorParams params) {
        return (lambda - params.getLambda0()) / params.getK();
    }

    private static void calculateZYB(int channelNumber, int decoderNumber,
                                    double lambda, SensorParams params,
                                    Map<String, Double> reviseValues) {
        List<Double> additionalParams = params.getAdditionalParams();
        if (additionalParams == null || additionalParams.isEmpty()) return;

        for (int i = 0; i < additionalParams.size(); i++) {
            double lambdaI = additionalParams.get(i);
            String reviseKey = String.format("Ch%d_rev%d", channelNumber, i+1);
            reviseValues.put(reviseKey, (lambda - lambdaI) / params.getK());
        }
    }
}