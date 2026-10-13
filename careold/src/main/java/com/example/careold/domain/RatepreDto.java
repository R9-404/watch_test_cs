package com.example.careold.domain;

public class RatepreDto {
    private String name;
    private int oldId;
    private String sex;
    private String rateState;
    private String preState;
    private String createTime;
    private int hr;
    private int bp;
    /** 记录ID（用于异常处置） */
    private Integer recordId;

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }
    /** 心率异常是否已处理：1-已处理 */
    private String rateHandled;
    /** 血压异常是否已处理：1-已处理 */
    private String preHandled;

    public String getRateHandled() {
        return rateHandled;
    }

    public void setRateHandled(String rateHandled) {
        this.rateHandled = rateHandled;
    }

    public String getPreHandled() {
        return preHandled;
    }

    public void setPreHandled(String preHandled) {
        this.preHandled = preHandled;
    }

    public int getHr() {
        return hr;
    }

    public void setHr(int hr) {
        this.hr = hr;
    }

    public int getBp() {
        return bp;
    }

    public void setBp(int bp) {
        this.bp = bp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOldId() {
        return oldId;
    }

    public void setOldId(int oldId) {
        this.oldId = oldId;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getRateState() {
        return rateState;
    }

    public void setRateState(String rateState) {
        this.rateState = rateState;
    }

    public String getPreState() {
        return preState;
    }

    public void setPreState(String preState) {
        this.preState = preState;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
