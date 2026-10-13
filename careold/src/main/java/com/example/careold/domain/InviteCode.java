package com.example.careold.domain;

/**
 * 邀请码
 * 由管理员生成，家属凭邀请码自助注册账号
 */
public class InviteCode {

    private Integer id;

    /** 邀请码 */
    private String code;

    /** 邀请码绑定的老人ID，家属注册后自动绑定该老人 */
    private Integer oldId;

    /** 生成人（管理员账号） */
    private String createUser;

    /** 生成时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;

    /** 过期时间 yyyy-MM-dd HH:mm:ss，为空表示长期有效 */
    private String expireTime;

    /** 是否已被使用：0-未使用 1-已使用 */
    private Integer used;

    /** 使用时间 yyyy-MM-dd HH:mm:ss */
    private String usedTime;

    /** 使用者的家属ID */
    private Integer usedFamilyId;

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

    public Integer getUsedFamilyId() {
        return usedFamilyId;
    }

    public void setUsedFamilyId(Integer usedFamilyId) {
        this.usedFamilyId = usedFamilyId;
    }
}
