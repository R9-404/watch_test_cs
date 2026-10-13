package com.example.careold.dao;

import com.example.careold.domain.PositionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PositionJdbcDao implements PositionDao {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public List<PositionDto> getPosition(String name) {
        String sql=null;
        List<PositionDto> positionDtos=null;
        // 位置取 device_location 最新一条，摔倒状态取 alarm_precess 最新一条
        // 时间列取“最近定位时间”和“最近告警时间”里更晚的那个，这样平时显示定位时间、摔倒后显示告警时间
        // 注意：MySQL 8 默认开启 ONLY_FULL_GROUP_BY，非分组列必须用 ANY_VALUE 包起来
        if(name==null || "".equals(name)){
            sql="SELECT ANY_VALUE(o.name) AS name, o.old_id AS oldId, ANY_VALUE(o.sex) AS sex,"
                    + " ANY_VALUE((SELECT l.lng FROM device_location l WHERE l.old_id=o.old_id ORDER BY l.id DESC LIMIT 1)) AS lng,"
                    + " ANY_VALUE((SELECT l.lat FROM device_location l WHERE l.old_id=o.old_id ORDER BY l.id DESC LIMIT 1)) AS lat,"
                    + " ANY_VALUE(GREATEST("
                    + "   COALESCE((SELECT MAX(l.be_time) FROM device_location l WHERE l.old_id=o.old_id), o.be_time),"
                    + "   COALESCE((SELECT MAX(a.be_time) FROM alarm_precess a WHERE a.old_id=o.old_id), o.be_time))) AS beTime,"
                    + " ANY_VALUE(COALESCE((SELECT a.status FROM alarm_precess a WHERE a.old_id=o.old_id ORDER BY a.be_time DESC, a.id DESC LIMIT 1), '0')) AS status"
                    + " FROM oldperson o GROUP BY o.old_id ORDER BY beTime DESC";
            positionDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(PositionDto.class));
        }else{
            sql="SELECT ANY_VALUE(o.name) AS name, o.old_id AS oldId, ANY_VALUE(o.sex) AS sex,"
                    + " ANY_VALUE((SELECT l.lng FROM device_location l WHERE l.old_id=o.old_id ORDER BY l.id DESC LIMIT 1)) AS lng,"
                    + " ANY_VALUE((SELECT l.lat FROM device_location l WHERE l.old_id=o.old_id ORDER BY l.id DESC LIMIT 1)) AS lat,"
                    + " ANY_VALUE(GREATEST("
                    + "   COALESCE((SELECT MAX(l.be_time) FROM device_location l WHERE l.old_id=o.old_id), o.be_time),"
                    + "   COALESCE((SELECT MAX(a.be_time) FROM alarm_precess a WHERE a.old_id=o.old_id), o.be_time))) AS beTime,"
                    + " ANY_VALUE(COALESCE((SELECT a.status FROM alarm_precess a WHERE a.old_id=o.old_id ORDER BY a.be_time DESC, a.id DESC LIMIT 1), '0')) AS status"
                    + " FROM oldperson o WHERE o.name like ? GROUP BY o.old_id ORDER BY beTime DESC";
            positionDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(PositionDto.class),"%"+name+"%");
        }
        return positionDtos;
    }

    @Override
    public List<PositionDto> getPositionFamily(int oldId) {
        String sql="SELECT ANY_VALUE(o.name) AS name, o.old_id AS oldId, ANY_VALUE(o.sex) AS sex,"
                + " ANY_VALUE((SELECT l.lng FROM device_location l WHERE l.old_id=o.old_id ORDER BY l.id DESC LIMIT 1)) AS lng,"
                + " ANY_VALUE((SELECT l.lat FROM device_location l WHERE l.old_id=o.old_id ORDER BY l.id DESC LIMIT 1)) AS lat,"
                + " ANY_VALUE(GREATEST("
                + "   COALESCE((SELECT MAX(l.be_time) FROM device_location l WHERE l.old_id=o.old_id), o.be_time),"
                + "   COALESCE((SELECT MAX(a.be_time) FROM alarm_precess a WHERE a.old_id=o.old_id), o.be_time))) AS beTime,"
                + " ANY_VALUE(COALESCE((SELECT a.status FROM alarm_precess a WHERE a.old_id=o.old_id ORDER BY a.be_time DESC, a.id DESC LIMIT 1), '0')) AS status"
                + " FROM oldperson o WHERE o.old_id=? GROUP BY o.old_id ORDER BY beTime DESC";
        List<PositionDto> positionDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(PositionDto.class),oldId);
        return positionDtos;
    }

}
