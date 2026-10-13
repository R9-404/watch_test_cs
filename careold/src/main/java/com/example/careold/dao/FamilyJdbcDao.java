package com.example.careold.dao;

import com.example.careold.domain.Family;
import com.example.careold.domain.FamilyDto;
import com.example.careold.domain.FamilyDto2;
import com.example.careold.domain.OldDto2;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class FamilyJdbcDao extends JdbcCom implements FamilyDao {


    @Override
    public List<Family> findById(String s) {
        String sql="";
        List<Family> families=null;
        if(s==null || "".equals(s)){
            sql="select * from family";
            families=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(Family.class));
        }else{
            sql="select * from family where family_name like ?";
            families=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(Family.class),"%"+s+"%");
        }
        return families;
    }

//    @Override
//    public List<Family> findById(String s) {
//        String sql="";
//        List<Family> families=null;
//        if(s==null || "".equals(s)){
//            sql="select old_id,name,oldperson.family_id,family_name,family_relation,family_phone,family_sex,family_address from oldperson INNER JOIN family on family.family_id=oldperson.family_id";
//            families=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(Family.class));
//        }else{
//            sql="select old_id,name,oldperson.family_id,family_name,family_relation,family_phone,family_sex,family_address from oldperson INNER JOIN family on family.family_id=oldperson.family_id where family_name like ?";
//            families=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(Family.class),"%"+s+"%");
//        }
//        return families;
//    }

    @Override
    public List<FamilyDto> findFamily(String s) {
        String sql="";
        List<FamilyDto> familyDtos=null;
        if(s==null || "".equals(s)){
            sql="select old_id,name,oldperson.family_id,family_name,family_relation,family_phone,family_sex,family_address from oldperson INNER JOIN family on family.family_id=oldperson.family_id";
            familyDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(FamilyDto.class));
        }else{
            sql="select old_id,name,oldperson.family_id,family_name,family_relation,family_phone,family_sex,family_address from oldperson INNER JOIN family on family.family_id=oldperson.family_id where family_name like ?";
            familyDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(FamilyDto.class),"%"+s+"%");
        }
        return familyDtos;
    }

    @Override
    public FamilyDto findFamilyByPhone(String phone) {
        String sql="SELECT old_id,name,oldperson.family_id,family_name,family_relation,family_phone,family_sex,family_address,family_tou_pic from oldperson inner JOIN family on family.family_id=oldperson.family_id where family_phone=?";
        // 家属账号存在但没有关联任何老人时，原来的 queryForObject 会抛
        // EmptyResultDataAccessException 让登录直接 500，这里改成返回 null 由调用方处理
        List<FamilyDto> list=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(FamilyDto.class),phone);
        return list.isEmpty()?null:list.get(0);
    }

    @Override
    public String findFamilyIdByPhone(String phone) {
        String sql="select  family_id from family where family_phone = ?";
        String familyId=jdbcTemplate.queryForObject(sql,String.class,phone);
        return familyId;
    }

    @Override
    public FamilyDto2 getFamilyByPhoneForFamily(String phone) {
        String sql="select family_id,family_name,family_sex,family_address,family_password,family_tou_pic from family where family_phone=?";
        FamilyDto2 familyDto2=jdbcTemplate.queryForObject(sql,new BeanPropertyRowMapper<>(FamilyDto2.class),phone);
        return familyDto2;
    }

    @Override
    public int updateFamilyByPhoneForFamily(FamilyDto2 familyDto2) {
        String sql="update family set family_name=?,family_sex=?,family_address=?,family_phone=? where family_id=?";
        int rows=jdbcTemplate.update(sql,familyDto2.getFamilyName(),familyDto2.getFamilySex(),familyDto2.getFamilyAddress(),familyDto2.getFamilyPhone(),familyDto2.getFamilyId());
        return rows;
    }

    @Override
    public int updateFamilyPasswordByPhoneForFamily(String password,String phone) {
        String sql="update family set family_password=? where family_phone=?";
        int rows=jdbcTemplate.update(sql,password,phone);
        return rows;
    }

    @Override
    public int updateFamilyTouPic(String pic,String phone) {
        String sql="update family set family_tou_pic=?  where family_phone=?";
         int rows=jdbcTemplate.update(sql,pic,phone);
        return rows;
    }

    @Override
    public OldDto2 findOldDto2ByPhone(String phone) {
        String sql="select old_id,name from oldperson where phone=?";
        OldDto2 oldDto2=jdbcTemplate.queryForObject(sql,new BeanPropertyRowMapper<>(OldDto2.class),phone);
        return oldDto2;
    }

    @Override
    public int save(Family family) {
        String sql="insert into family(family_name,family_relation,family_phone,family_sex,family_address) VALUES(?,?,?,?,?) ";
        int rows=jdbcTemplate.update(sql,family.getFamilyName(),family.getFamilyRelation(),family.getFamilyPhone(),family.getFamilySex(),family.getFamilyAddress());
        return rows;
    }

    @Override
    public Integer saveAndGetId(Family family) {
        String sql = "insert into family(family_name,family_relation,family_phone,family_sex,family_address,family_password)"
                + " VALUES(?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int rows = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"family_id"});
            ps.setString(1, family.getFamilyName());
            ps.setString(2, family.getFamilyRelation());
            ps.setString(3, family.getFamilyPhone());
            ps.setString(4, family.getFamilySex());
            ps.setString(5, family.getFamilyAddress());
            ps.setString(6, family.getFamilyPassword());
            return ps;
        }, keyHolder);
        if (rows != 1 || keyHolder.getKey() == null) {
            return null;
        }
        return keyHolder.getKey().intValue();
    }

    @Override
    public int countByPhone(String phone) {
        String sql = "select count(1) from family where family_phone = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, phone);
        return count == null ? 0 : count;
    }

    @Override
    public int update(Family family) {        String sql="UPDATE family SET family_name=?,family_relation=?,family_phone=?,family_sex=?,family_address=? WHERE family_id=?";
        int rows=jdbcTemplate.update(sql,family.getFamilyName(),family.getFamilyRelation(),family.getFamilyPhone(),family.getFamilySex(),family.getFamilyAddress(),family.getFamilyId());
        return rows;
    }

    @Override
    public int update2(Family family) {
        String sql="UPDATE family SET family_password=?,family_tou_pic=? WHERE family_id=?";
        int rows=jdbcTemplate.update(sql,family.getFamilyPassword(),family.getFamilyTouPic(),family.getFamilyId());
        return rows;
    }

    @Override
    public int delete(String s) {
        String sql="DELETE FROM family WHERE family_id=?";
        int rows=jdbcTemplate.update(sql,s);
        return rows;
    }
}
