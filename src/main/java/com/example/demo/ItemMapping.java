package com.example.demo;

import java.util.HashMap;
import java.util.Map;

public class ItemMapping {
    public static final Map<String, String> COLUMN_MAPPING = new HashMap<>();

    static {
        // 列名映射
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

    }
}