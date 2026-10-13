package com.example.careold.domain;

/**
 * 设备绑定码：在手表上输入该码即可把设备绑定到老人
 */
public class BindCode {

    private Integer id;

    private String code;

    private Integer oldId;

    private Integer familyId;

    private String createUser;

    private String createTime;

    private String expireTime;

    private Integer used;

    private String usedTime;

    private Integer usedDeviceId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getUsed() {
        return used;
    }

    public void setUsed(Integer used) {
        this.used = used;
    }

    public String getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(String usedTime) {
        this.usedTime = usedTime;
    }

    public Integer getUsedDeviceId() {
        return usedDeviceId;
    }

    public void setUsedDeviceId(Integer usedDeviceId) {
        this.usedDeviceId = usedDeviceId;
    }
}
