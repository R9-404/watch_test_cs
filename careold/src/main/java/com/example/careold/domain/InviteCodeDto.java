package com.example.careold.domain;

/**
 * 邀请码（含绑定老人姓名），用于后台邀请码列表展示
 */
public class InviteCodeDto extends InviteCode {

    /** 绑定的老人姓名 */
    private String oldPersonName;

    public String getOldPersonName() {
        return oldPersonName;
    }

    public void setOldPersonName(String oldPersonName) {
        this.oldPersonName = oldPersonName;
    }
}
