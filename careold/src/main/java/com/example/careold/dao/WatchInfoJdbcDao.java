package com.example.careold.dao;

import com.example.careold.domain.OldPerson;
import com.example.careold.domain.WatchDto;
import com.example.careold.domain.WatchInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WatchInfoJdbcDao implements WatchInfoDao {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public List<WatchDto> getWatchInfo(String oldName) {
        String sql="";
        List<WatchDto> watchDtos=null;
        if(oldName==null || "".equals(oldName)){
            sql="select watch_info.device_id,op_time,hrf,bpf,hrl,bpl,name from watch_info left join oldperson on watch_info.device_id=oldperson.device_id ";
            watchDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(WatchDto.class));
        }else{
            sql="select watch_info.device_id,op_time,hrf,bpf,hrl,bpl,name from watch_info left join oldperson on watch_info.device_id=oldperson.device_id  where name like ?";
            watchDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(WatchDto.class),"%"+oldName+"%");
        }
        return watchDtos;
    }

    @Override
    public List<WatchDto> getWatchInfo2(int deviceId) {
        String sql="";
        List<WatchDto> watchDtos=null;
        sql="select watch_info.device_id,op_time,hrf,bpf,hrl,bpl,name from watch_info left join oldperson on watch_info.device_id=oldperson.device_id  where watch_info.device_id=?";
        watchDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(WatchDto.class),deviceId);
        return watchDtos;
    }

    @Override
    public int addWatchInfo(WatchInfo watchInfo) {
        String sql="insert into watch_info(device_id,op_time,bpf,bpl,hrl,hrf) values(?,?,?,?,?,?)";
        int rows=jdbcTemplate.update(sql,watchInfo.getDeviceId(),watchInfo.getOpTime(),watchInfo.getBpf(),watchInfo.getBpl(),watchInfo.getHrl(),watchInfo.getHrf());
        return rows;
    }

    @Override
    public int updateWatchInfo(WatchInfo watchInfo) {
        String sql="update watch_info set bpf=?,bpl=?,hrl=?,hrf=? where device_id=?";
        int rows=jdbcTemplate.update(sql,watchInfo.getBpf(),watchInfo.getBpl(),watchInfo.getHrl(),watchInfo.getHrf(),watchInfo.getDeviceId());
        return rows;
    }

    @Override
    public int deleteWatchInfo(String deviceId) {
        String sql="delete from watch_info where device_id=?";
        int rows=jdbcTemplate.update(sql,deviceId);
        return rows;
    }
}
