package com.example.careold.dao;

import com.example.careold.common.DateConst;
import com.example.careold.domain.InviteCode;
import com.example.careold.domain.InviteCodeDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class InviteCodeJdbcDao extends JdbcCom implements InviteCodeDao {

    /**
     * 邀请码表的字段
     * 1. 用 AS 显式转成驼峰别名，兼容旧版 BeanPropertyRowMapper（不依赖驼峰自动映射配置）
     * 2. 日期统一用 DATE_FORMAT 返回字符串，前端可直接展示，也避免时区/时间戳问题
     */
    private static final String COLUMNS =
            "i.id as id, i.code as code, i.old_id as oldId, i.create_user as createUser,"
            + " DATE_FORMAT(i.create_time,'%Y-%m-%d %H:%i:%s') AS createTime,"
            + " DATE_FORMAT(i.expire_time,'%Y-%m-%d %H:%i:%s') AS expireTime,"
            + " i.used as used,"
            + " DATE_FORMAT(i.used_time,'%Y-%m-%d %H:%i:%s') AS usedTime,"
            + " i.used_family_id as usedFamilyId";

    private BeanPropertyRowMapper<InviteCodeDto> rowMapper() {
        return new BeanPropertyRowMapper<>(InviteCodeDto.class);
    }

    @Override
    public List<InviteCodeDto> findInviteCodes(int used) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ").append(COLUMNS).append(", o.name as oldPersonName")
                .append(" from invite_code i")
                .append(" left join oldperson o on o.old_id = i.old_id");
        if (used == 0 || used == 1) {
            sql.append(" where i.used = ?");
        }
        sql.append(" order by i.id desc");

        if (used == 0 || used == 1) {
            return jdbcTemplate.query(sql.toString(), rowMapper(), used);
        }
        return jdbcTemplate.query(sql.toString(), rowMapper());
    }

    @Override
    public InviteCodeDto findByCode(String code) {
        String sql = "select " + COLUMNS + ", o.name as oldPersonName"
                + " from invite_code i"
                + " left join oldperson o on o.old_id = i.old_id"
                + " where i.code = ?";
        List<InviteCodeDto> list = jdbcTemplate.query(sql, rowMapper(), code);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public Integer save(InviteCode inviteCode) {
        String sql = "insert into invite_code(code, old_id, create_user, create_time, expire_time, used)"
                + " values(:code, :oldId, :createUser, :createTime, :expireTime, 0)";

        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("code", inviteCode.getCode());
        paramMap.addValue("oldId", inviteCode.getOldId());
        paramMap.addValue("createUser", inviteCode.getCreateUser());
        paramMap.addValue("createTime", new Date());
        paramMap.addValue("expireTime", inviteCode.getExpireTime());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        // 这里用 NamedParameterJdbcTemplate 只为方便写具名参数，底层共用同一个数据源
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
        int rows = namedJdbcTemplate.update(sql, paramMap, keyHolder,
                new String[]{"id"});
        if (rows != 1 || keyHolder.getKey() == null) {
            return null;
        }
        return keyHolder.getKey().intValue();
    }

    @Override
    public int markUsed(Integer id, Integer familyId, String usedTime) {
        String sql = "update invite_code set used = 1, used_family_id = ?, used_time = ? where id = ? and used = 0";
        return jdbcTemplate.update(sql, familyId, usedTime, id);
    }

    @Override
    public int deleteUnused(Integer id) {
        String sql = "delete from invite_code where id = ? and used = 0";
        return jdbcTemplate.update(sql, id);
    }

    @Override
    public int deleteByOldId(Integer oldId) {
        String sql = "delete from invite_code where old_id = ? and used = 0";
        return jdbcTemplate.update(sql, oldId);
    }

    /**
     * 生成不重复的邀请码：8 位大写字母+数字（去掉 0/O/1/I 等易混淆字符）
     */
    @Override
    public String generateCode() {
        final String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        java.util.Random random = new java.util.Random();
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String code = sb.toString();
            if (findByCode(code) == null) {
                return code;
            }
        }
        // 极小概率走到这里，用时间戳兜底
        return "IV" + DateConst.dateToString(new Date(), DateConst.DATE_MODEL_8);
    }
}
