package com.example.careold.domain;

public class WatchInfo {
    private Integer deviceId;
    private String opTime;
    private Integer bpf;
    private Integer bpl;
    private Integer hrf;
    private Integer hrl;


    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getOpTime() {
        return opTime;
    }

    public void setOpTime(String opTime) {
        this.opTime = opTime;
    }

    public Integer getBpf() {
        return bpf;
    }

    public void setBpf(Integer bpf) {
        this.bpf = bpf;
    }

    public Integer getBpl() {
        return bpl;
    }

    public void setBpl(Integer bpl) {
        this.bpl = bpl;
    }

    public Integer getHrf() {
        return hrf;
    }

    public void setHrf(Integer hrf) {
        this.hrf = hrf;
    }

    public Integer getHrl() {
        return hrl;
    }

    public void setHrl(Integer hrl) {
        this.hrl = hrl;
    }
}
