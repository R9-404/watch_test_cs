package com.example.careold.dao;

import com.example.careold.domain.RatepreDto;
import com.example.careold.domain.StepDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StepJdbcDao implements StepDao {

    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * 说明：原来的 SQL 用 GROUP BY 取数据，在 MySQL 8 下会随机取到旧记录（取不到最新一条）。
     * 这里改成「先取每台设备最新一条 watch_day_info 的日期，再回表取该天数据」，
     * 表格与曲线都显示最新上报的步数/睡眠。
     */
    private static final String LATEST_SELECT =
            "SELECT o.name AS name, o.old_id AS oldId, o.sex AS sex, w.step AS step, w.sleep AS sleep,"
            + " w.sleep_sate AS sleepSate, DATE_FORMAT(w.create_time,'%Y-%m-%d') AS createTime"
            + " FROM oldperson o"
            + " INNER JOIN watch_day_info w ON w.device_id = o.device_id"
            + " INNER JOIN (SELECT device_id, MAX(create_time) AS mt FROM watch_day_info GROUP BY device_id) m"
            + " ON m.device_id = w.device_id AND m.mt = w.create_time";

    @Override
    public List<StepDto>  getStepSleep(String name, String timeFirst, String timeLast) {
        String sql;
        List<StepDto> stepDtos=null;
        boolean noName = (name==null || "".equals(name));
        boolean noTime = (timeFirst==null || "".equals(timeFirst)) || (timeLast==null || "".equals(timeLast));
        if(noName && noTime){
            sql="SELECT * FROM (" + LATEST_SELECT + ") t ORDER BY t.createTime DESC";
            stepDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(StepDto.class));
        }else if(noName){
            sql="SELECT * FROM (" + LATEST_SELECT + ") t WHERE t.createTime BETWEEN ? AND ? ORDER BY t.createTime DESC";
            stepDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(StepDto.class),timeFirst,timeLast);
        }else{
            sql="SELECT * FROM (" + LATEST_SELECT + ") t WHERE t.name like ? ORDER BY t.createTime DESC";
            stepDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(StepDto.class),"%"+name+"%");
        }

        return stepDtos;
    }

    @Override
    public List<StepDto> getStepSleepFamily(int oldId) {
        String sql;
        List<StepDto> stepDtos=null;
        sql="SELECT * FROM (" + LATEST_SELECT + ") t WHERE t.oldId=? ORDER BY t.createTime DESC";
        stepDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(StepDto.class),oldId);

        return stepDtos;
    }

    @Override
    public List<String> getSleepBadCount() {
        String sql="select device_id deviceId from watch_day_info where sleep_sate='1' group by device_id";
        List<String> deviceIdList=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(String.class));
        return deviceIdList;
    }

    @Override
    public List<StepDto> getStepSleepDetail(int oldId) {
        String sql="SELECT o.name AS name, o.old_id AS oldId, o.sex AS sex, w.step AS step, w.sleep AS sleep,"
                + " w.sleep_sate AS sleepSate, DATE_FORMAT(w.create_time,'%Y-%m-%d') AS createTime"
                + " FROM oldperson o INNER JOIN watch_day_info w ON w.device_id = o.device_id"
                + " WHERE o.old_id=? ORDER BY w.create_time ASC LIMIT 7";
        List<StepDto> stepDtos=jdbcTemplate.query(sql,new BeanPropertyRowMapper<>(StepDto.class),oldId);
        return stepDtos;
    }
}
