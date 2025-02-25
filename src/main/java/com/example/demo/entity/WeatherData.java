package com.example.demo.entity;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true)
@Measurement(name = "weather_data")
public class WeatherData {

    @Column(name = "_time", timestamp = true)
    private Instant timestamp;

    @Column(name = "ambient_temperature")
    private double ambientTemperature;

    @Column(name = "temperature1")
    private double temperature1;

    @Column(name = "dew_point_temperature")
    private double dewPointTemperature;

    @Column(name = "ambient_humidity")
    private double ambientHumidity;

    @Column(name = "air_pressure")
    private double airPressure;

    @Column(name = "total_radiation1_instant")
    private double totalRadiation1Instant;

    @Column(name = "uv_radiation_instant")
    private double UVRadiationInstant;

    @Column(name = "wind_direction")
    private double windDirection;

    @Column(name = "instant_wind_speed")
    private double instantWindSpeed;

    @Column(name = "windspeed_2min")
    private double windSpeed2Min;

    @Column(name = "windspeed_10min")
    private double windSpeed10Min;

    @Column(name = "rainfall_interval_accumulated")
    private double rainfallIntervalAccumulated;

    @Column(name = "rainfall_daily_accumulated")
    private double rainfallDailyAccumulated;

    @Column(name = "total_radiation1_daily_accumulated")
    private double totalRadiation1DailyAccumulated;

    @Column(name = "uv_radiation_daily_accumulated")
    private double UVRadiationDailyAccumulated;

    @Column(name = "illuminance")
    private double illuminance;

    @Column(name = "voltage")
    private double voltage;
}
