package com.example.careold.dao;

import com.example.careold.domain.OldPerson;

import java.util.List;

public interface OldPersonDao {
    List<OldPerson> getOldPerson(String name);

    /**
     * 按老人ID查询老人信息（不存在返回 null）
     */
    OldPerson getOldPersonById(int oldId);

    int addOldPerson(OldPerson oldPerson);

    int updateOldPerson(OldPerson oldPerson);

    int deleteOldPerson(int oldPersonId);

    int updateOldPerson2(int oldId, int familyId);

    /**
     * 把老人档案上登记的手表设备号改成指定设备（老人与手表的绑定关系）
     */
    int updateOldPersonDeviceId(int oldId, int deviceId);
}
