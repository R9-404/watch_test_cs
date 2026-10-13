package com.example.careold.domain;


public class AlarmProcess {
    private int id;
    private String status;
    private int oldId;
    private int deviceId;
    private String beTime;
    private String lng;//精度
    private String lat;//维度

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOldId() {
        return oldId;
    }

    public void setOldId(int oldId) {
        this.oldId = oldId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getBeTime() {
        return beTime;
    }

    public void setBeTime(String beTime) {
        this.beTime = beTime;
    }


}
