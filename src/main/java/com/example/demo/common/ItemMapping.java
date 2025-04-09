package com.example.demo.common;

import com.example.demo.entity.SensorParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemMapping {
    public static final Map<String, String> COLUMN_MAPPING = new HashMap<>();

    static {
        // 列名映射
        // 动态称重
        COLUMN_MAPPING.put("编号", "id");
        COLUMN_MAPPING.put("时间", "timestamp");
        COLUMN_MAPPING.put("总重KG", "weightKg");
        COLUMN_MAPPING.put("车长", "vehicleLength");
        COLUMN_MAPPING.put("车道", "lane");
        COLUMN_MAPPING.put("轴数", "axleCount");
        COLUMN_MAPPING.put("速度", "speed");
        COLUMN_MAPPING.put("温度", "temperature");
        COLUMN_MAPPING.put("方向", "direction");
        COLUMN_MAPPING.put("跨道", "crossLane");
        COLUMN_MAPPING.put("类型", "type");
        COLUMN_MAPPING.put("轴重1", "axleWeight1");
        COLUMN_MAPPING.put("轴重2", "axleWeight2");
        COLUMN_MAPPING.put("轴重3", "axleWeight3");
        COLUMN_MAPPING.put("轴重4", "axleWeight4");
        COLUMN_MAPPING.put("轴重5", "axleWeight5");
        COLUMN_MAPPING.put("轴重6", "axleWeight6");
        COLUMN_MAPPING.put("轴距1", "wheelbase1");
        COLUMN_MAPPING.put("轴距2", "wheelbase2");
        COLUMN_MAPPING.put("轴距3", "wheelbase3");
        COLUMN_MAPPING.put("轴距4", "wheelbase4");
        COLUMN_MAPPING.put("轴距5", "wheelbase5");
        COLUMN_MAPPING.put("车型编号", "vehicleTypeCode");
        COLUMN_MAPPING.put("车型", "vehicleType");
        COLUMN_MAPPING.put("轴1KN", "axle1Kn");
        COLUMN_MAPPING.put("轴2KN", "axle2Kn");
        COLUMN_MAPPING.put("轴3KN", "axle3Kn");
        COLUMN_MAPPING.put("偏移", "offset");

        // 气象数据
//        COLUMN_MAPPING.put("时间", "timestamp");
        COLUMN_MAPPING.put("环温(℃)", "ambientTemperature");
        COLUMN_MAPPING.put("温度1(℃)", "temperature1");
        COLUMN_MAPPING.put("露点温度(℃)", "dewPointTemperature");
        COLUMN_MAPPING.put("环湿(%RH)", "ambientHumidity");
        COLUMN_MAPPING.put("气压(hPa)", "airPressure");
        COLUMN_MAPPING.put("总辐射1瞬时(W/㎡)", "totalRadiation1Instant");
        COLUMN_MAPPING.put("紫外辐射瞬时(W/㎡)", "UVRadiationInstant");
        COLUMN_MAPPING.put("风向(°)", "windDirection");
        COLUMN_MAPPING.put("瞬时风速(m/s)", "instantWindSpeed");
        COLUMN_MAPPING.put("2分钟风速(m/s)", "windSpeed2Min");
        COLUMN_MAPPING.put("10分钟风速(m/s)", "windSpeed10Min");
        COLUMN_MAPPING.put("雨量间隔累计(mm)", "rainfallIntervalAccumulated");
        COLUMN_MAPPING.put("雨量日累计(mm)", "rainfallDailyAccumulated");
        COLUMN_MAPPING.put("总辐射1日累计(MJ/㎡)", "totalRadiation1DailyAccumulated");
        COLUMN_MAPPING.put("紫外辐射日累计(MJ/㎡)", "UVRadiationDailyAccumulated");
        COLUMN_MAPPING.put("光照度(KLux)", "illuminance");
        COLUMN_MAPPING.put("电量(V)", "voltage");

        // 沉降数据
        COLUMN_MAPPING.put("测点", "id");
//        COLUMN_MAPPING.put("时间", "timestamp");
        COLUMN_MAPPING.put("累计变化位移", "subside");

        // 孔隙水压力数据
//        COLUMN_MAPPING.put("测点", "id");
//        COLUMN_MAPPING.put("时间", "timestamp");
        COLUMN_MAPPING.put("累计变化压力", "waterPressure");

        // 温湿度数据
//        COLUMN_MAPPING.put("测点", "id");
//        COLUMN_MAPPING.put("时间", "timestamp");
        COLUMN_MAPPING.put("温度", "temperature");
        COLUMN_MAPPING.put("湿度", "wet");
    }

    public static final Map<String, SensorParams> sensorParamsMap = new HashMap<>();

    static {
        sensorParamsMap.put("01_Ch1", new SensorParams(1538.150, 0.830735, 0.009703, 0.02527, 2));
        sensorParamsMap.put("01_Ch2", new SensorParams(1540.707, 0.741034, 0.009703, 0.02728, 2));
        sensorParamsMap.put("01_Ch3", new SensorParams(1543.897, 1.518299, 0.009703, 0.02738, 2));
        sensorParamsMap.put("01_Ch4", new SensorParams(1550.805, 0.001120, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch5", new SensorParams(1565.245, 0.001182, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch6", new SensorParams(1538.717, 0.00130, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch7", new SensorParams(1547.915, 0.001193, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch8", new SensorParams(0.0, 0.001149, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch9", new SensorParams(1550.892, 0.001198, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch10", new SensorParams(1540.044, 0.009703, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch11", new SensorParams(1548.037, 0.010000, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch12", new SensorParams(1537.370, 0.946645, 0.009754, 0.02416, 3));
        sensorParamsMap.put("01_Ch13", new SensorParams(1540.418, 0.843174, 0.009754, 0.02728, 3));
        sensorParamsMap.put("01_Ch14", new SensorParams(1543.671, 1.299844, 0.009754, 0.02671, 3));
        sensorParamsMap.put("01_Ch15", new SensorParams(1555.250, 0.001101, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch16", new SensorParams(1535.205, 0.001184, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch17", new SensorParams(1548.114, 0.001190, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch18", new SensorParams(1538.905, 0.001121, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch19", new SensorParams(1554.453, 0.001154, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch20", new SensorParams(1533.112, 0.001154, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch21", new SensorParams(1550.852, 0.009754, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch22", new SensorParams(1554.903, 0.009764, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch23", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8, List.of(1529.645,1534.488,1538.975,1542.847,1549.705,1554.711)));
        sensorParamsMap.put("01_Ch24", new SensorParams(0.0, 0.0012, 1.0, 1.0, 0));
        sensorParamsMap.put("01_Ch25", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1529.528,1534.065)));
        sensorParamsMap.put("01_Ch26", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1539.712,1543.036,1549.300,1554.618)));
        sensorParamsMap.put("01_Ch27", new SensorParams(1537.130, 0.662987, 0.00964, 0.02547, 0));
        sensorParamsMap.put("01_Ch28", new SensorParams(1539.818, 1.502559, 0.00964, 0.02737, 0));
        sensorParamsMap.put("01_Ch29", new SensorParams(1543.889, 1.035090, 0.00964, 0.02681, 0));
        sensorParamsMap.put("01_Ch30", new SensorParams(1554.823, 0.001142, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch31", new SensorParams(1556.772, 0.001124, 1.0, 1.0, 1));
        sensorParamsMap.put("01_Ch32", new SensorParams(1540.122, 0.001125, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch1", new SensorParams(1561.683, 0.001168, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch2", new SensorParams(1559.894, 0.001103, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch3", new SensorParams(1547.811, 0.001192, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch4", new SensorParams(1535.009, 0.001128, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch5", new SensorParams(1542.115, 0.001127, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch6", new SensorParams(1544.498, 0.001158, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch7", new SensorParams(1547.601, 0.001191, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch8", new SensorParams(1550.436, 0.001195, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch9", new SensorParams(1554.020, 0.001124, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch10", new SensorParams(1534.754, 0.001105, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch11", new SensorParams(1538.407, 0.001156, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch12", new SensorParams(1541.500, 0.001143, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch13", new SensorParams(1547.560, 0.001188, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch14", new SensorParams(1550.450, 0.001146, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch15", new SensorParams(1553.527, 0.001106, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch16", new SensorParams(1556.982, 0.001112, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch17", new SensorParams(1556.698, 0.001144, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch18", new SensorParams(1559.341, 0.001173, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch19", new SensorParams(1562.874, 0.001118, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch20", new SensorParams(1553.875, 0.00964, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch21", new SensorParams(1538.702, 0.00962, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch22", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1529.148,1533.814,1537.012,1542.722,1549.245,1553.962)));
        sensorParamsMap.put("02_Ch23", new SensorParams(0.0, 0.0012, 1.0, 1.0, 0));
        sensorParamsMap.put("02_Ch24", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1529.855,1534.574,1538.843,1542.549,1549.001,1553.285)));
        sensorParamsMap.put("02_Ch25", new SensorParams(0.0, 0.0012, 1.0, 1.0, 0));
        sensorParamsMap.put("02_Ch26", new SensorParams(1562.452, 0.00979, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch27", new SensorParams(1529.760, 0.00951, 1.0, 1.0, 1));
        sensorParamsMap.put("02_Ch28", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("02_Ch29", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("02_Ch30", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("02_Ch31", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("02_Ch32", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("03_Ch1", new SensorParams(1537.870, 0.801305, 0.009785, 0.02623, 5));
        sensorParamsMap.put("03_Ch2", new SensorParams(1540.865, 1.134332, 0.009785, 0.02713, 5));
        sensorParamsMap.put("03_Ch3", new SensorParams(1552.560, 0.877574, 0.009785, 0.02641, 5));
        sensorParamsMap.put("03_Ch4", new SensorParams(1556.498, 0.001102, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch5", new SensorParams(1540.061, 0.001147, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch6", new SensorParams(1542.006, 0.001173, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch7", new SensorParams(1541.511, 0.001131, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch8", new SensorParams(1562.782, 0.001138, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch9", new SensorParams(1546.227, 0.001107, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch10", new SensorParams(1549.809, 0.009785, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch11", new SensorParams(1553.807, 0.010370, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch12", new SensorParams(1538.174, 1.522705, 0.009857, 0.02868, 6));
        sensorParamsMap.put("03_Ch13", new SensorParams(1542.928, 1.227569, 0.02597, 0.009857, 6));
        sensorParamsMap.put("03_Ch14", new SensorParams(1551.088, 0.838668, 0.009857, 0.02592, 6));
        sensorParamsMap.put("03_Ch15", new SensorParams(1539.920, 0.001103, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch16", new SensorParams(1556.524, 0.001103, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch17", new SensorParams(1550.959, 0.001147, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch18", new SensorParams(1560.002, 0.001174, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch19", new SensorParams(1562.782, 0.001107, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch20", new SensorParams(1549.655, 0.001193, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch21", new SensorParams(1556.838, 0.009857, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch22", new SensorParams(1547.811, 0.0983, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch23", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1529.411,1533.599,1537.111,1542.503,1549.380,1553.975)));
        sensorParamsMap.put("03_Ch24", new SensorParams(0.0, 0.0012, 1.0, 1.0, 0));
        sensorParamsMap.put("03_Ch25", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1529.639,1534.548)));
        sensorParamsMap.put("03_Ch26", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1539.086,1544.705,1549.113,1554.048)));
        sensorParamsMap.put("03_Ch27", new SensorParams(1538.587, 0.917356, 0.00974, 0.02099, 0));
        sensorParamsMap.put("03_Ch28", new SensorParams(1543.069, 1.534425, 0.00974, 0.02524, 0));
        sensorParamsMap.put("03_Ch29", new SensorParams(1551.255, 1.565445, 0.02874, 0.00974, 0));
        sensorParamsMap.put("03_Ch30", new SensorParams(1535.631, 0.001143, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch31", new SensorParams(1529.946, 0.001111, 1.0, 1.0, 1));
        sensorParamsMap.put("03_Ch32", new SensorParams(1535.973, 0.001185, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch1", new SensorParams(1535.464, 0.001159, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch2", new SensorParams(1565.610, 0.001118, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch3", new SensorParams(1544.708, 0.001193, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch4", new SensorParams(1534.997, 0.001195, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch5", new SensorParams(1541.634, 0.001132, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch6", new SensorParams(1544.643, 0.001198, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch7", new SensorParams(1547.973, 0.001169, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch8", new SensorParams(1553.828, 0.001154, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch9", new SensorParams(1556.725, 0.001126, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch10", new SensorParams(1535.126, 0.001131, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch11", new SensorParams(1538.864, 0.001122, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch12", new SensorParams(1544.667, 0.001160, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch13", new SensorParams(1547.820, 0.001166, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch14", new SensorParams(1550.669, 0.001126, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch15", new SensorParams(1553.681, 0.001158, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch16", new SensorParams(1556.393, 0.001199, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch17", new SensorParams(1544.995, 0.001135, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch18", new SensorParams(1560.099, 0.001170, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch19", new SensorParams(1562.756, 0.001163, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch20", new SensorParams(1559.858, 0.00974, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch21", new SensorParams(1541.881, 0.00965, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch22", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1529.473,1533.463,1537.495,1543.118,1549.187,1554.132)));
        sensorParamsMap.put("04_Ch23", new SensorParams(0.0, 0.0012, 1.0, 1.0, 0));
        sensorParamsMap.put("04_Ch24", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1529.197,1534.510)));
        sensorParamsMap.put("04_Ch25", new SensorParams(0.0, 0.0012, 1.0, 1.0, 8,List.of(1539.577,1543.151,1549.466,1554.588)));
        sensorParamsMap.put("04_Ch26", new SensorParams(1535.545, 0.00985, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch27", new SensorParams(1554.944, 0.01042, 1.0, 1.0, 1));
        sensorParamsMap.put("04_Ch28", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("04_Ch29", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("04_Ch30", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("04_Ch31", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
        sensorParamsMap.put("04_Ch32", new SensorParams(0.0, 1.0, 1.0, 1.0, 0));
    }
}