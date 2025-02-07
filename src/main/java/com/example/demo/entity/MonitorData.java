package com.example.demo.entity;


public class MonitorData {
    private String monitorDeviceCode;
    private String value;
    private String originalValue;
    private DateTime time;

    // Getters and setters

    public String getMonitorDeviceCode() {
        return monitorDeviceCode;
    }

    public void setMonitorDeviceCode(String monitorDeviceCode) {
        this.monitorDeviceCode = monitorDeviceCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }
}
