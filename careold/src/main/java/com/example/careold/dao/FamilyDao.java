package com.example.careold.dao;

import com.example.careold.domain.Family;
import com.example.careold.domain.FamilyDto;
import com.example.careold.domain.FamilyDto2;
import com.example.careold.domain.OldDto2;

import java.util.List;

public interface FamilyDao extends CrudRepository<Family,String>{

    int update2(Family family);

    public List<FamilyDto> findFamily(String s);

    FamilyDto findFamilyByPhone(String phone);

    OldDto2 findOldDto2ByPhone(String phone);

    String findFamilyIdByPhone(String phone);

    FamilyDto2 getFamilyByPhoneForFamily(String phone);

    int updateFamilyTouPic(String pic,String phone);

    int updateFamilyByPhoneForFamily(FamilyDto2 familyDto2);

    int updateFamilyPasswordByPhoneForFamily(String password,String phone);

    /**
     * 新增家属并返回自增主键 family_id（失败返回 null）
     */
    Integer saveAndGetId(Family family);

    /**
     * 统计该手机号是否已存在家属账号
     */
    int countByPhone(String phone);

}
