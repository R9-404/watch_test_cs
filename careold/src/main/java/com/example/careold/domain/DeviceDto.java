package com.example.careold.domain;

/**
 * 设备档案 + 绑定老人信息，用于后台设备列表展示
 */
public class DeviceDto extends Device {

    /** 绑定老人姓名 */
    private String oldPersonName;

    /** 绑定老人电话 */
    private String oldPersonPhone;

    public String getOldPersonName() {
        return oldPersonName;
    }

    public void setOldPersonName(String oldPersonName) {
        this.oldPersonName = oldPersonName;
    }

    public String getOldPersonPhone() {
        return oldPersonPhone;
    }

    public void setOldPersonPhone(String oldPersonPhone) {
        this.oldPersonPhone = oldPersonPhone;
    }
}
