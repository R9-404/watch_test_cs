package com.example.careold.dao;

import com.example.careold.common.DateConst;
import com.example.careold.domain.BindCode;
import com.example.careold.domain.Device;
import com.example.careold.domain.DeviceDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Random;

@Repository
public class DeviceJdbcDao extends JdbcCom implements DeviceDao {

    /** 设备号/绑定码去掉了 0/O/1/I 等易混淆字符，方便老人在手表上输入 */
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private static final String DEVICE_COLUMNS =
            "d.device_id as deviceId, d.device_no as deviceNo, d.secret as secret, d.alias as alias,"
            + " d.model as model, d.app_version as appVersion, d.battery as battery,"
            + " d.old_id as oldId, d.family_id as familyId, d.status as status,"
            + " DATE_FORMAT(d.bind_time,'%Y-%m-%d %H:%i:%s') AS bindTime,"
            + " DATE_FORMAT(d.last_time,'%Y-%m-%d %H:%i:%s') AS lastTime,"
            + " d.last_lng as lastLng, d.last_lat as lastLat,"
            + " DATE_FORMAT(d.create_time,'%Y-%m-%d %H:%i:%s') AS createTime,"
            + " d.person_handled as personHandled, d.rate_handled as rateHandled, d.pre_handled as preHandled";

    private static final String BIND_CODE_COLUMNS =
            "id, code, old_id as oldId, family_id as familyId, create_user as createUser,"
            + " DATE_FORMAT(create_time,'%Y-%m-%d %H:%i:%s') AS createTime,"
            + " DATE_FORMAT(expire_time,'%Y-%m-%d %H:%i:%s') AS expireTime,"
            + " used, DATE_FORMAT(used_time,'%Y-%m-%d %H:%i:%s') AS usedTime,"
            + " used_device_id as usedDeviceId";

    private BeanPropertyRowMapper<Device> deviceMapper() {
        return new BeanPropertyRowMapper<>(Device.class);
    }

    private BeanPropertyRowMapper<BindCode> bindCodeMapper() {
        return new BeanPropertyRowMapper<>(BindCode.class);
    }

    @Override
    public Device findByDeviceNo(String deviceNo) {
        String sql = "select " + DEVICE_COLUMNS + " from device d where d.device_no = ?";
        List<Device> list = jdbcTemplate.query(sql, deviceMapper(), deviceNo);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Device findById(Integer deviceId) {
        String sql = "select " + DEVICE_COLUMNS + " from device d where d.device_id = ?";
        List<Device> list = jdbcTemplate.query(sql, deviceMapper(), deviceId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<DeviceDto> findDevices() {
        String sql = "select " + DEVICE_COLUMNS + ", o.name as oldPersonName, o.phone as oldPersonPhone"
                + " from device d left join oldperson o on o.old_id = d.old_id"
                + " order by d.device_id";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(DeviceDto.class));
    }

    @Override
    public Integer save(Device device) {
        String sql = "insert into device(device_no, secret, alias, model, old_id, status, bind_time, create_time)"
                + " values(:deviceNo, :secret, :alias, :model, :oldId, :status, :bindTime, :createTime)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("deviceNo", device.getDeviceNo());
        params.addValue("secret", device.getSecret());
        params.addValue("alias", device.getAlias());
        params.addValue("model", device.getModel());
        params.addValue("oldId", device.getOldId());
        params.addValue("status", device.getStatus() == null ? 1 : device.getStatus());
        params.addValue("bindTime", device.getOldId() == null ? null : new Date());
        params.addValue("createTime", new Date());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
        int rows = named.update(sql, params, keyHolder, new String[]{"device_id"});
        if (rows != 1 || keyHolder.getKey() == null) {
            return null;
        }
        return keyHolder.getKey().intValue();
    }

    @Override
    public int update(Device device) {
        String sql = "update device set alias = ?, model = ?, old_id = ?, family_id = ?, status = ?,"
                + " bind_time = ?, last_time = ?, last_lng = ?, last_lat = ?, battery = ?, app_version = ?"
                + " where device_id = ?";
        return jdbcTemplate.update(sql, device.getAlias(), device.getModel(), device.getOldId(),
                device.getFamilyId(), device.getStatus(), toTimestamp(device.getBindTime()),
                toTimestamp(device.getLastTime()), device.getLastLng(), device.getLastLat(),
                device.getBattery(), device.getAppVersion(), device.getDeviceId());
    }

    @Override
    public int updateSecret(Integer deviceId, String secret) {
        return jdbcTemplate.update("update device set secret = ? where device_id = ?", secret, deviceId);
    }

    @Override
    public int touch(Integer deviceId, Integer battery, String appVersion, String lastTime) {
        String sql = "update device set last_time = ?, battery = coalesce(?, battery),"
                + " app_version = coalesce(?, app_version) where device_id = ?";
        return jdbcTemplate.update(sql, lastTime, battery, appVersion, deviceId);
    }

    @Override
    public int updateLastPosition(Integer deviceId, String lng, String lat) {
        return jdbcTemplate.update("update device set last_lng = ?, last_lat = ? where device_id = ?",
                lng, lat, deviceId);
    }

    @Override
    public int saveWatchProfile(Integer deviceId, Integer hrf, Integer hrl, Integer bpf, Integer bpl, String opTime) {
        // watch_info 是老的设备档案表：device_id 主键，存心率/血压的正常上下限
        int rows = jdbcTemplate.update(
                "update watch_info set hrf = ?, hrl = ?, bpf = ?, bpl = ?, op_time = ? where device_id = ?",
                hrf, hrl, bpf, bpl, opTime, deviceId);
        if (rows == 0) {
            rows = jdbcTemplate.update(
                    "insert into watch_info(device_id, op_time, bpf, bpl, hrf, hrl) values(?,?,?,?,?,?)",
                    deviceId, opTime, bpf, bpl, hrf, hrl);
        }
        return rows;
    }

    @Override
    public int unbind(Integer deviceId) {
        return jdbcTemplate.update("update device set old_id = null, family_id = null, bind_time = null,"
                + " last_lng = null, last_lat = null where device_id = ?", deviceId);
    }

    @Override
    public int delete(Integer deviceId) {
        return jdbcTemplate.update("delete from device where device_id = ?", deviceId);
    }

    @Override
    public int countByOldId(Integer oldId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from device where old_id = ?", Integer.class, oldId);
        return count == null ? 0 : count;
    }

    // ==================== 绑定码 ====================

    @Override
    public Integer saveBindCode(BindCode bindCode) {
        String sql = "insert into bind_code(code, old_id, family_id, create_user, create_time, expire_time, used)"
                + " values(:code, :oldId, :familyId, :createUser, :createTime, :expireTime, 0)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("code", bindCode.getCode());
        params.addValue("oldId", bindCode.getOldId());
        params.addValue("familyId", bindCode.getFamilyId());
        params.addValue("createUser", bindCode.getCreateUser());
        params.addValue("createTime", new Date());
        params.addValue("expireTime", toTimestamp(bindCode.getExpireTime()));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
        int rows = named.update(sql, params, keyHolder, new String[]{"id"});
        if (rows != 1 || keyHolder.getKey() == null) {
            return null;
        }
        return keyHolder.getKey().intValue();
    }

    @Override
    public BindCode findBindCode(String code) {
        String sql = "select " + BIND_CODE_COLUMNS + " from bind_code where code = ?";
        List<BindCode> list = jdbcTemplate.query(sql, bindCodeMapper(), code);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int markBindCodeUsed(Integer id, Integer deviceId, String usedTime) {
        String sql = "update bind_code set used = 1, used_device_id = ?, used_time = ? where id = ? and used = 0";
        return jdbcTemplate.update(sql, deviceId, usedTime, id);
    }

    @Override
    public List<BindCode> findBindCodesByOldId(Integer oldId) {
        String sql = "select " + BIND_CODE_COLUMNS + " from bind_code where old_id = ? order by id desc limit 20";
        return jdbcTemplate.query(sql, bindCodeMapper(), oldId);
    }

    @Override
    public int deleteUnusedBindCode(Integer id) {
        return jdbcTemplate.update("delete from bind_code where id = ? and used = 0", id);
    }

    // ==================== 上报落库 ====================

    @Override
    public int insertLocation(Integer deviceId, Integer oldId, String lng, String lat, String source, String beTime) {
        String sql = "insert into device_location(device_id, old_id, lng, lat, source, be_time, create_time)"
                + " values(?,?,?,?,?,?,NOW())";
        return jdbcTemplate.update(sql, deviceId, oldId, lng, lat, source, toTimestamp(beTime));
    }

    @Override
    public int insertAlarm(Integer oldId, Integer deviceId, String lng, String lat, String beTime) {
        String sql = "insert into alarm_precess(status, old_id, device_id, be_time, lng, lat)"
                + " values('1',?,?,?,?,?)";
        return jdbcTemplate.update(sql, oldId, deviceId, toTimestamp(beTime), lng, lat);
    }

    @Override
    public int updateLatestAlarmStatus(Integer oldId, String status) {
        // MySQL 8 不再支持 UPDATE ... ORDER BY ... LIMIT，这里先取最近一条告警的 id 再更新
        List<Integer> ids = jdbcTemplate.queryForList(
                "select id from alarm_precess where old_id = ? order by be_time desc, id desc limit 1",
                Integer.class, oldId);
        if (ids.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.update("update alarm_precess set status = ? where id = ?", status, ids.get(0));
    }

    @Override
    public int saveDayInfo(Integer deviceId, int step, int sleep, String sleepState, String day) {
        // 每台设备每天只保留一条：先按 (device_id, create_time) 去重，再用 ON DUPLICATE KEY 原子更新。
        // 直接用 UPDATE+INSERT 在并发上报时会写进重复行，导致页面取到旧数据。
        dedupeDayInfo(deviceId, day);
        String sql = "insert into watch_day_info(device_id, sleep, step, sleep_sate, create_time)"
                + " values(?,?,?,?,?) on duplicate key update sleep = values(sleep), step = values(step),"
                + " sleep_sate = values(sleep_sate)";
        return jdbcTemplate.update(sql, deviceId, sleep, step, sleepState, day);
    }

    /** 删掉同设备同一天的重复记录，只保留 id 最大的一条 */
    private void dedupeDayInfo(Integer deviceId, String day) {
        jdbcTemplate.update(
                "delete from watch_day_info where device_id = ? and create_time = ?"
                        + " and id < (select max_id from (select max(id) as max_id from watch_day_info"
                        + " where device_id = ? and create_time = ?) tmp)",
                deviceId, day, deviceId, day);
    }

    // ==================== 异常（红点）查询与处置 ====================

    /** 异常判定参数：与 DeviceController 的阈值保持一致 */
    private static final int HR_LOW = 55;
    private static final int HR_HIGH = 110;
    private static final int BP_LOW = 90;
    private static final int BP_HIGH = 140;
    private static final int OFFLINE_SECONDS = 120;
    private static final int LOW_BATTERY = 20;

    @Override
    public java.util.Map<String, Object> findPendingAlert(Integer deviceId, String kind) {
        if ("person".equals(kind)) {
            return findPersonAlert(deviceId);
        }
        if ("rate".equals(kind) || "pre".equals(kind)) {
            return findRatepreAlert(deviceId, kind);
        }
        return null;
    }

    /** 人员（设备）不合格：未绑定老人 / 长时间离线 / 电量低 / 缺少心率血压阈值 */
    private java.util.Map<String, Object> findPersonAlert(Integer deviceId) {
        String sql = "select d.device_id as deviceId, d.device_no as deviceNo, d.alias as alias,"
                + " d.old_id as oldId, DATE_FORMAT(d.last_time,'%Y-%m-%d %H:%i:%s') as lastTime,"
                + " d.battery as battery, d.boot_seq as bootSeq, d.handled_seq as handledSeq,"
                + " DATE_FORMAT(d.create_time,'%Y-%m-%d %H:%i:%s') as createTime, o.name as oldPersonName"
                + " from device d left join oldperson o on o.old_id = d.old_id where d.device_id = ?";
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(sql, deviceId);
        if (rows.isEmpty()) {
            return null;
        }
        java.util.Map<String, Object> device = rows.get(0);

        Integer oldId = device.get("oldId") == null ? null : toInt(device.get("oldId"));
        String lastTime = device.get("lastTime") == null ? null : device.get("lastTime").toString();
        Integer battery = device.get("battery") == null ? null : toInt(device.get("battery"));

        // 人员不合格红点是否显示：用"掉线次数"判断，避免时间戳比较的误差
        //   boot_seq > handled_seq  -> 设备重新上线后又掉线了，需要重新报警
        //   否则说明这次问题已经人工确认过，不再重复提醒
        int bootSeq = toInt(device.get("bootSeq"));
        int handledSeq = toInt(device.get("handledSeq"));
        boolean onlineNow = isOnline(lastTime);
        // 已经人工确认过、且设备没有重新上线过 -> 不再重复报"离线/从未上报"
        // （但未绑定、电量低、没配阈值等问题仍然要提示）
        boolean offlineAcknowledged = handledSeq >= bootSeq && !onlineNow;

        List<String> reasons = new java.util.ArrayList<String>();
        List<String> details = new java.util.ArrayList<String>();
        if (oldId == null) {
            reasons.add("未绑定老人");
            details.add("设备还没有绑定到老人，采集到的数据无法归到具体人身上。");
        }
        Date last = DateConst.toDate(lastTime, DateConst.DATE_MODEL_2);
        long offlineSec = last == null ? -1 : (System.currentTimeMillis() - last.getTime()) / 1000;
        if (!offlineAcknowledged) {
            if (last == null) {
                reasons.add("从未上报");
                details.add("设备登记后一直没有心跳和数据上报，可能是手表未开机或服务器地址填错。");
            } else if (offlineSec > OFFLINE_SECONDS) {
                reasons.add("离线 " + humanSeconds(offlineSec));
                details.add("最后一次心跳是 " + lastTime + "，超过 " + (OFFLINE_SECONDS / 60) + " 分钟没有上报。");
            }
        }
        if (battery != null && battery > 0 && battery < LOW_BATTERY) {
            reasons.add("电量低（" + battery + "%）");
            details.add("电量低于 " + LOW_BATTERY + "%，需要提醒老人充电。");
        }
        // 阈值缺失：没配心率/血压上下限时，异常判定没有依据
        List<java.util.Map<String, Object>> profile = jdbcTemplate.queryForList(
                "select hrf, hrl, bpf, bpl from watch_info where device_id = ?", deviceId);
        if (profile.isEmpty()
                || profile.get(0).get("hrf") == null || profile.get(0).get("hrl") == null
                || profile.get(0).get("bpf") == null || profile.get(0).get("bpl") == null) {
            reasons.add("未配置心率血压阈值");
            details.add("该设备在「手表信息」里没有心率/血压的正常上下限，异常判定没有依据，请先补全。");
        }
        if (reasons.isEmpty()) {
            return null;
        }

        java.util.Map<String, Object> row = new java.util.LinkedHashMap<String, Object>();
        row.put("kind", "person");
        row.put("kindText", "人员不合格");
        row.put("reason", join(reasons));
        row.put("detail", join(details));
        row.put("value", "");
        row.put("threshold", "");
        row.put("occurTime", lastTime == null ? device.get("createTime") : lastTime);
        row.put("recordId", null);
        row.put("deviceId", deviceId);
        row.put("deviceNo", device.get("deviceNo"));
        row.put("alias", device.get("alias"));
        row.put("oldPersonName", oldId == null || device.get("oldPersonName") == null
                ? "（未绑定）" : device.get("oldPersonName").toString());
        return row;
    }

    /** 心率/血压不合格：该设备"最近一次处理之后"新产生的异常记录 */
    private java.util.Map<String, Object> findRatepreAlert(Integer deviceId, String kind) {
        boolean isRate = "rate".equals(kind);
        String stateColumn = isRate ? "rate_state" : "pre_state";
        String handledAtColumn = isRate ? "rate_handled_at" : "pre_handled_at";
        String valueColumn = isRate ? "hr" : "bp";

        // 关键点：只有 create_time 晚于"上次处理时间"的异常才算新的未处理异常，
        // 否则手表每几秒上报一次的旧异常会让红点永远消不掉。
        String sql = "select id, device_id as deviceId, old_id as oldId, hr, bp,"
                + " DATE_FORMAT(create_time,'%Y-%m-%d %H:%i:%s') as createTime"
                + " from ratepre where device_id = ? and " + stateColumn + " = '1'"
                + " and (" + handledAtColumn + " is null or create_time > " + handledAtColumn + ")"
                + " order by create_time desc, id desc limit 1";
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(sql, deviceId);
        if (rows.isEmpty()) {
            return null;
        }
        java.util.Map<String, Object> data = rows.get(0);
        int value = toInt(data.get(valueColumn));

        java.util.Map<String, Object> row = new java.util.LinkedHashMap<String, Object>();
        row.put("kind", kind);
        row.put("kindText", isRate ? "心率不合格" : "血压不合格");
        row.put("reason", isRate
                ? (value > HR_HIGH ? "心率偏高" : "心率偏低")
                : (value > BP_HIGH ? "血压偏高" : "血压偏低"));
        row.put("detail", isRate
                ? "正常范围 " + HR_LOW + "-" + HR_HIGH + " 次/分，本次测量 " + value + " 次/分。"
                : "正常范围 " + BP_LOW + "-" + BP_HIGH + " mmHg，本次测量 " + value + " mmHg。");
        row.put("value", value);
        row.put("threshold", isRate ? (HR_LOW + "-" + HR_HIGH) : (BP_LOW + "-" + BP_HIGH));
        row.put("occurTime", data.get("createTime"));
        row.put("recordId", data.get("id"));
        row.put("deviceId", deviceId);
        Object oldId = data.get("oldId");
        row.put("oldPersonName", oldId == null ? "（未绑定）" : oldPersonName(toInt(oldId)));
        return row;
    }

    @Override
    public List<java.util.Map<String, Object>> countPendingAlerts() {
        // 未处理数量 = 该设备"上次处理时间"之后新产生的异常条数
        String sql = "select device_id as deviceId, device_no as deviceNo, alias, old_id as oldId,"
                + " last_time as lastTime, battery,"
                + " (select count(1) from ratepre r where r.device_id = d.device_id and r.rate_state = '1'"
                + "    and (r.rate_handled_at is null or r.create_time > r.rate_handled_at)) as rateCount,"
                + " (select count(1) from ratepre r where r.device_id = d.device_id and r.pre_state = '1'"
                + "    and (r.pre_handled_at is null or r.create_time > r.pre_handled_at)) as preCount"
                + " from device d where d.status = 1 order by d.device_id";
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        // 人员不合格需要按同样规则判断，在 Java 侧补齐
        java.util.List<java.util.Map<String, Object>> result =
                new java.util.ArrayList<java.util.Map<String, Object>>();
        for (java.util.Map<String, Object> raw : rows) {
            int deviceId = toInt(raw.get("deviceId"));
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<String, Object>();
            item.put("deviceId", deviceId);
            item.put("deviceNo", raw.get("deviceNo"));
            item.put("alias", raw.get("alias"));
            item.put("personCount", findPersonAlert(deviceId) == null ? 0 : 1);
            item.put("rateCount", toInt(raw.get("rateCount")));
            item.put("preCount", toInt(raw.get("preCount")));
            result.add(item);
        }
        return result;
    }

    @Override
    public int resolveDeviceAlert(Integer deviceId, String kind) {
        if ("person".equals(kind)) {
            // 记录"已经确认过的掉线次数"：只有设备重新上线后又掉线（次数变大）才会再次报警
            return jdbcTemplate.update(
                    "update device set person_handled = 1, person_handled_at = NOW(),"
                            + " handled_seq = boot_seq where device_id = ?", deviceId);
        }
        if ("rate".equals(kind) || "pre".equals(kind)) {
            // 处置某类异常时，把该设备该类"已有"的异常全部归入已处理（记录处理时间），
            // 这样历史异常不再重复亮红点，只有之后新产生的异常才会再次报警
            return resolveRatepreAlert(deviceId, kind, null);
        }
        return 0;
    }

    /**
     * 设备被确认在线（心跳/上报接口调用）：记下确认时间，供"掉线识别"使用。
     * 这样判断完全是数据库里的确定性比较，不依赖内存状态。
     */
    public void markOnline(Integer deviceId) {
        jdbcTemplate.update("update device set last_online_at = NOW() where device_id = ?", deviceId);
    }

    /**
     * 扫描所有设备，识别"离线"，见方法说明
     * 如果设备最后在线时间晚于最后上报时间（说明是恢复在线之后又掉线了），
     * 就把掉线次数 +1、清除"已确认"标记（人员不合格红点重新出现），并把最后在线时间清空，
     * 保证同一次掉线只计一次（否则每次刷新页面都会重复计数）。
     */
    public void scanOfflineDevices() {
        jdbcTemplate.update("update device set boot_seq = boot_seq + 1, person_handled = 0,"
                + " last_online_at = null"
                + " where status = 1 and last_online_at is not null and last_time is not null"
                + " and last_online_at > last_time"
                + " and last_time < DATE_SUB(NOW(), INTERVAL " + OFFLINE_SECONDS + " SECOND)");
    }

    private void logState(Integer deviceId, String message) {
        org.slf4j.LoggerFactory.getLogger(DeviceJdbcDao.class).debug("[设备状态] deviceId={} {}", deviceId, message);
    }

    @Override
    public int resolveRatepreAlert(Integer deviceId, String kind, Integer recordId) {
        boolean isRate = "rate".equals(kind);
        String stateColumn = isRate ? "rate_state" : "pre_state";
        String handledColumn = isRate ? "rate_handled" : "pre_handled";
        String handledAtColumn = isRate ? "rate_handled_at" : "pre_handled_at";

        if (recordId != null) {
            return jdbcTemplate.update("update ratepre set " + handledColumn + " = 1, "
                    + handledAtColumn + " = NOW(), " + stateColumn + " = '0' where id = ?", recordId);
        }
        return jdbcTemplate.update("update ratepre set " + handledColumn + " = 1, "
                + handledAtColumn + " = NOW(), " + stateColumn + " = '0'"
                + " where device_id = ? and " + stateColumn + " = '1'"
                + " and (" + handledAtColumn + " is null or create_time > " + handledAtColumn + ")", deviceId);
    }

    /** 处置后如果该设备已经没有未处理的同类异常，顺便把设备层面的标记也置上 */
    public void clearDeviceFlagIfNoAlert(Integer deviceId, String kind) {
        if (findRatepreAlert(deviceId, kind) == null) {
            String column = "rate".equals(kind) ? "rate_handled" : "pre_handled";
            jdbcTemplate.update("update device set " + column + " = 1 where device_id = ?", deviceId);
        }
    }

    private String oldPersonName(Integer oldId) {
        if (oldId == null) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
                "select name from oldperson where old_id = ?", String.class, oldId);
        return names.isEmpty() ? "" : names.get(0);
    }

    private String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private String humanSeconds(long seconds) {
        if (seconds < 60) {
            return seconds + " 秒";
        }
        if (seconds < 3600) {
            return (seconds / 60) + " 分钟";
        }
        return (seconds / 3600) + " 小时";
    }

    /** 在线判定：默认 120 秒内有心跳算在线（与 OFFLINE_SECONDS 一致） */
    private boolean isOnline(String lastTime) {
        Date last = DateConst.toDate(lastTime, DateConst.DATE_MODEL_2);
        return last != null && (System.currentTimeMillis() - last.getTime()) <= OFFLINE_SECONDS * 1000L;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private java.sql.Timestamp toTimestamp(String time) {
        if (time == null || time.trim().length() == 0) {
            return null;
        }
        Date date = DateConst.toDate(time.trim(), DateConst.DATE_MODEL_2);
        return date == null ? null : new java.sql.Timestamp(date.getTime());
    }

    /** 生成不重复的设备号（W + 8 位） */
    public String generateDeviceNo() {
        Random random = new Random();
        for (int attempt = 0; attempt < 30; attempt++) {
            StringBuilder sb = new StringBuilder("W");
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (findByDeviceNo(sb.toString()) == null) {
                return sb.toString();
            }
        }
        return "W" + DateConst.dateToString(new Date(), DateConst.DATE_MODEL_8);
    }

    /** 生成不重复的绑定码（6 位，老人好输入） */
    public String generateBindCode() {
        Random random = new Random();
        for (int attempt = 0; attempt < 30; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            if (findBindCode(sb.toString()) == null) {
                return sb.toString();
            }
        }
        return DateConst.dateToString(new Date(), DateConst.DATE_MODEL_9);
    }

    /** 生成设备密钥 */
    public String generateSecret() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
