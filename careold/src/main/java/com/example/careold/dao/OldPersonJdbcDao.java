package com.example.careold.dao;

import com.example.careold.domain.OldPerson;
import com.example.careold.domain.PositionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OldPersonJdbcDao implements OldPersonDao {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public List<OldPerson> getOldPerson(String name) {
        String sql="";
        List<OldPerson> oldPeople=null;
        if(name==null || "".equals(name)){
            sql="select * from oldperson";
            oldPeople=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(OldPerson.class));
        }else{
            sql="select * from oldperson where name like ?";
            oldPeople=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(OldPerson.class),"%"+name+"%");
        }
        return oldPeople;
    }

    @Override
    public OldPerson getOldPersonById(int oldId) {
        String sql = "select * from oldperson where old_id = ?";
        List<OldPerson> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(OldPerson.class), oldId);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public int addOldPerson(OldPerson oldPerson) {
        String sql="insert into oldperson(name,age,sex,phone,room,illness,warn,food_like,food_hate,drag,customer_id,be_time,bothdate,device_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
        int rows=jdbcTemplate.update(sql,oldPerson.getName(),oldPerson.getAge(),oldPerson.getSex(),oldPerson.getPhone(),oldPerson.getRoom(),oldPerson.getIllness(),oldPerson.getWarn(),oldPerson.getFoodLike(),oldPerson.getFoodHate(),oldPerson.getDrag(),oldPerson.getCustomerId(),oldPerson.getBeTime(),oldPerson.getBothdate(),oldPerson.getDeviceId());
        return rows;
    }

    @Override
    public int updateOldPerson(OldPerson oldPerson) {
        String sql="update oldperson set name=?,age=?,sex=?,phone=?,room=?,illness=?,warn=?,food_like=?,food_hate=?,drag=?,family_id=?,customer_id=?,bothdate=?,device_id=? where old_id=?";
        int rows=jdbcTemplate.update(sql,oldPerson.getName(),oldPerson.getAge(),oldPerson.getSex(),oldPerson.getPhone(),oldPerson.getRoom(),oldPerson.getIllness(),oldPerson.getWarn(),oldPerson.getFoodLike(),oldPerson.getFoodHate(),oldPerson.getDrag(),oldPerson.getFamilyId(),oldPerson.getCustomerId(),oldPerson.getBothdate(),oldPerson.getDeviceId(),oldPerson.getOldId());
        return rows;
    }

    @Override
    public int updateOldPerson2(int oldId, int familyId) {
        String sql="update oldperson set family_id=? where old_id=?";
        int rows=jdbcTemplate.update(sql,familyId,oldId);
        return rows;
    }

    @Override
    public int updateOldPersonDeviceId(int oldId, int deviceId) {
        String sql="update oldperson set device_id=? where old_id=?";
        int rows=jdbcTemplate.update(sql,deviceId,oldId);
        return rows;
    }

    @Override
    public int deleteOldPerson(int oldPersonId) {
        String sql="delete from oldperson where old_id=?";
        int rows=jdbcTemplate.update(sql,oldPersonId);
        return rows;
    }
}
