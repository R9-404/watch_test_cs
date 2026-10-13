package com.example.careold.domain;

/**
 * 手表设备档案
 */
public class Device {

    private Integer deviceId;

    /** 设备号：手表端登录用 */
    private String deviceNo;

    /** 设备密钥：手表端登录用 */
    private String secret;

    /** 别名，如「爷爷的手表」 */
    private String alias;

    /** 型号 */
    private String model;

    /** 手表端应用版本 */
    private String appVersion;

    /** 电量百分比 */
    private Integer battery;

    /** 绑定的老人ID */
    private Integer oldId;

    /** 绑定操作人（家属ID） */
    private Integer familyId;

    /** 1-启用 0-停用 */
    private Integer status;

    /** 绑定时间 yyyy-MM-dd HH:mm:ss */
    private String bindTime;

    /** 最后一次心跳时间 */
    private String lastTime;

    /** 最近上报的经度 */
    private String lastLng;

    /** 最近上报的纬度 */
    private String lastLat;

    /** 创建时间 */
    private String createTime;

    /** 人员（设备）不合格是否已处理 */
    private Integer personHandled;

    /** 心率不合格是否已处理 */
    private Integer rateHandled;

    /** 血压不合格是否已处理 */
    private Integer preHandled;

    public Integer getPersonHandled() {
        return personHandled;
    }

    public void setPersonHandled(Integer personHandled) {
        this.personHandled = personHandled;
    }

    public Integer getRateHandled() {
        return rateHandled;
    }

    public void setRateHandled(Integer rateHandled) {
        this.rateHandled = rateHandled;
    }

    public Integer getPreHandled() {
        return preHandled;
    }

    public void setPreHandled(Integer preHandled) {
        this.preHandled = preHandled;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceNo() {
        return deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        this.deviceNo = deviceNo;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public Integer getBattery() {
        return battery;
    }

    public void setBattery(Integer battery) {
        this.battery = battery;
    }

    public Integer getOldId() {
        return oldId;
    }

    public void setOldId(Integer oldId) {
        this.oldId = oldId;
    }

    public Integer getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Integer familyId) {
        this.familyId = familyId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getBindTime() {
        return bindTime;
    }

    public void setBindTime(String bindTime) {
        this.bindTime = bindTime;
    }

    public String getLastTime() {
        return lastTime;
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public String getLastLng() {
        return lastLng;
    }

    public void setLastLng(String lastLng) {
        this.lastLng = lastLng;
    }

    public String getLastLat() {
        return lastLat;
    }

    public void setLastLat(String lastLat) {
        this.lastLat = lastLat;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
