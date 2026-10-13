package com.example.careold.controller;

import com.example.careold.common.DateConst;
import com.example.careold.common.ReturnCodeUtil;
import com.example.careold.dao.DeviceDao;
import com.example.careold.dao.DeviceJdbcDao;
import com.example.careold.dao.OldPersonDao;import com.example.careold.domain.BindCode;
import com.example.careold.domain.Device;
import com.example.careold.domain.DeviceDto;
import com.example.careold.domain.OldPerson;
import org.apache.commons.collections.map.ListOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 设备侧（手表端）接口
 * 手表端流程：login（拿设备号+密钥登录）-> bind（输入绑定码绑定老人）-> heartbeat（心跳）-> report（上报数据）
 * 后台流程：list/createBindCode/resetSecret/unbind/delete/updateAlarmStatus
 */
@Controller
@RequestMapping("/restful/device")
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    /** 设备端错误码，沿用 1111 成功 / 0000 失败的约定 */
    private static final String AUTH_FAIL = "0004";
    private static final String NOT_BOUND = "0005";
    private static final String BIND_CODE_BAD = "0006";

    /** 心率/血压/步数的合理范围，防止异常数据写坏库 */
    private static final int HR_MIN = 20;
    private static final int HR_MAX = 250;
    private static final int BP_MIN = 40;
    private static final int BP_MAX = 300;
    private static final int STEP_MAX = 200000;

    /** watch_info 里维护的默认正常阈值（心率/血压） */
    private static final int DEFAULT_HRF = 55;
    private static final int DEFAULT_HRL = 110;
    private static final int DEFAULT_BPF = 90;
    private static final int DEFAULT_BPL = 140;

    @Autowired
    DeviceDao deviceDao;

    @Autowired
    DeviceJdbcDao deviceJdbcDao;

    @Autowired
    OldPersonDao oldPersonDao;

    /** 直接落库的简单写入（心率血压记录），复杂查询都在 DAO 里 */
    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // ==================== 手表端 ====================

    /**
     * 设备登录：校验设备号 + 密钥，返回设备信息与绑定状态
     * 入参：deviceNo、secret
     */
    @ResponseBody
    @PostMapping("/login")
    public ListOrderedMap login(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String deviceNo = paramValue(param, "deviceNo", "").toUpperCase();
        String secret = paramValue(param, "secret", "");

        Device device = deviceDao.findByDeviceNo(deviceNo);
        if (device == null || !secret.equals(device.getSecret())) {
            result.put(ReturnCodeUtil.returnCode, AUTH_FAIL);
            result.put("message", "设备号或密钥不正确");
            return result;
        }
        if (device.getStatus() != null && device.getStatus() == 0) {
            result.put(ReturnCodeUtil.returnCode, AUTH_FAIL);
            result.put("message", "设备已停用，请联系管理员");
            return result;
        }

        deviceDao.touch(device.getDeviceId(), null, null,
                DateConst.dateToString(new Date(), DateConst.DATE_MODEL_2));

        result.put("deviceId", device.getDeviceId());
        result.put("deviceNo", device.getDeviceNo());
        result.put("alias", device.getAlias());
        result.put("bound", device.getOldId() != null);
        result.put("oldId", device.getOldId());
        if (device.getOldId() != null) {
            OldPerson oldPerson = oldPersonDao.getOldPersonById(device.getOldId());
            result.put("oldPersonName", oldPerson == null ? "" : oldPerson.getName());
        }
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 绑定老人：手表上输入 6 位绑定码
     * 入参：deviceNo、secret、code
     */
    @ResponseBody
    @PostMapping("/bind")
    public ListOrderedMap bind(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        Device device = authDevice(param, result);
        if (device == null) {
            return result;
        }

        String code = paramValue(param, "code", "").replace(" ", "").replace("-", "").toUpperCase();
        if (code.length() == 0) {
            return fail(result, BIND_CODE_BAD, "请输入绑定码");
        }

        BindCode bindCode = deviceDao.findBindCode(code);
        if (bindCode == null) {
            return fail(result, BIND_CODE_BAD, "绑定码不存在，请在系统里重新生成");
        }
        if (bindCode.getUsed() != null && bindCode.getUsed() == 1) {
            return fail(result, BIND_CODE_BAD, "该绑定码已被使用");
        }
        if (isExpired(bindCode.getExpireTime())) {
            return fail(result, BIND_CODE_BAD, "该绑定码已过期，请重新生成");
        }

        OldPerson oldPerson = oldPersonDao.getOldPersonById(bindCode.getOldId());
        if (oldPerson == null) {
            return fail(result, ReturnCodeUtil.falseCode, "绑定码对应的老人不存在");
        }

        String now = DateConst.dateToString(new Date(), DateConst.DATE_MODEL_2);
        device.setOldId(oldPerson.getOldId());
        device.setFamilyId(bindCode.getFamilyId());
        device.setStatus(device.getStatus() == null ? 1 : device.getStatus());
        device.setBindTime(now);
        device.setLastTime(now);
        deviceDao.update(device);

        // 老人档案里记上这块表，保证心率/血压/步数页面能关联到设备。
        // 注意：老人档案只能记一个 device_id（页面按它关联数据），
        // 所以只有在老人还没登记设备时才写入，避免后来的表把原来那块表的数据“顶掉”。
        if (oldPerson.getDeviceId() == null) {
            oldPersonDao.updateOldPersonDeviceId(oldPerson.getOldId(), device.getDeviceId());
        }
        if (oldPerson.getFamilyId() == null && bindCode.getFamilyId() != null) {
            oldPersonDao.updateOldPerson2(oldPerson.getOldId(), bindCode.getFamilyId());
        }
        // 老的设备档案表也要有记录，否则手表信息页面看不到
        deviceDao.saveWatchProfile(device.getDeviceId(), DEFAULT_HRF, DEFAULT_HRL,
                DEFAULT_BPF, DEFAULT_BPL, now.substring(0, 10));

        int rows = deviceDao.markBindCodeUsed(bindCode.getId(), device.getDeviceId(), now);
        if (rows != 1) {
            return fail(result, BIND_CODE_BAD, "该绑定码已被使用");
        }

        result.put("oldId", oldPerson.getOldId());
        result.put("oldPersonName", oldPerson.getName());
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 心跳：手表定时调用，用于判断在线状态，并获取是否有待处理指令
     * 入参：deviceNo、secret、battery、appVersion
     */
    @ResponseBody
    @PostMapping("/heartbeat")
    public ListOrderedMap heartbeat(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        Device device = authDevice(param, result);
        if (device == null) {
            return result;
        }

        Integer battery = toInt(paramValue(param, "battery", ""), 0, 100);
        String appVersion = paramValue(param, "appVersion", "");
        String now = DateConst.dateToString(new Date(), DateConst.DATE_MODEL_2);
        deviceDao.touch(device.getDeviceId(), battery, appVersion, now);
        // 记下"设备在线"，供掉线判定使用（重新上线后才会在下次掉线时重新报警）
        deviceJdbcDao.markOnline(device.getDeviceId());

        result.put("serverTime", now);
        result.put("bound", device.getOldId() != null);
        result.put("oldId", device.getOldId());
        result.put("reportInterval", 5);
        result.put("pendingAlarm", 0);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 数据上报：心率、血压、步数、睡眠、位置、摔倒/告警
     * 入参：deviceNo、secret、hr、bp、step、sleep、sleepState、lng、lat、source、beTime、alarm、battery
     */
    @ResponseBody
    @PostMapping("/report")
    public ListOrderedMap report(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        Device device = authDevice(param, result);
        if (device == null) {
            return result;
        }
        if (device.getOldId() == null) {
            return fail(result, NOT_BOUND, "设备还未绑定老人，请先在手表上输入绑定码");
        }

        Integer oldId = device.getOldId();
        String now = DateConst.dateToString(new Date(), DateConst.DATE_MODEL_2);
        String beTime = paramValue(param, "beTime", now);
        if (DateConst.toDate(beTime, DateConst.DATE_MODEL_2) == null) {
            beTime = now;
        }
        String day = beTime.length() >= 10 ? beTime.substring(0, 10) : now.substring(0, 10);

        int hr = toInt(paramValue(param, "hr", ""), HR_MIN, HR_MAX);
        int bp = toInt(paramValue(param, "bp", ""), BP_MIN, BP_MAX);
        int step = toInt(paramValue(param, "step", ""), 0, STEP_MAX);
        int sleep = toInt(paramValue(param, "sleep", ""), 0, 24);
        String sleepState = paramValue(param, "sleepState", "0");
        String lng = paramValue(param, "lng", "");
        String lat = paramValue(param, "lat", "");
        String source = paramValue(param, "source", "gps");
        String alarm = paramValue(param, "alarm", "");

        // 规则判断（阶段4 会换成端侧模型，这里先用阈值规则）
        String rateState = (hr > 0 && (hr < DEFAULT_HRF || hr > DEFAULT_HRL)) ? "1" : "0";
        String preState = (bp > 0 && (bp < DEFAULT_BPF || bp > DEFAULT_BPL)) ? "1" : "0";

        if (hr > 0 || bp > 0) {
            insertRatepre(device.getDeviceId(), oldId, rateState, preState, beTime, hr, bp);
        }
        if (step > 0 || sleep > 0) {
            deviceDao.saveDayInfo(device.getDeviceId(), step, sleep, sleepState, day);
        }
        if (lng.length() > 0 && lat.length() > 0) {
            deviceDao.insertLocation(device.getDeviceId(), oldId, lng, lat, source, beTime);
            deviceDao.updateLastPosition(device.getDeviceId(), lng, lat);
        }
        if ("1".equals(alarm) || "fall".equalsIgnoreCase(alarm)) {
            // 摔倒：写告警记录（页面「位置信息-是否摔倒」直接展示）
            String alarmLng = lng.length() > 0 ? lng : device.getLastLng();
            String alarmLat = lat.length() > 0 ? lat : device.getLastLat();
            deviceDao.insertAlarm(oldId, device.getDeviceId(), alarmLng, alarmLat, beTime);
            logger.warn("收到摔倒告警：oldId={}, deviceId={}", oldId, device.getDeviceId());
        }

        deviceDao.touch(device.getDeviceId(), toInt(paramValue(param, "battery", ""), 0, 100),
                paramValue(param, "appVersion", ""), now);
        deviceJdbcDao.markOnline(device.getDeviceId());

        result.put("rateState", rateState);
        result.put("preState", preState);
        result.put("serverTime", now);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * SOS 一键呼叫：手表上按住按钮触发，直接生成告警
     * 入参：deviceNo、secret、lng、lat
     */
    @ResponseBody
    @PostMapping("/sos")
    public ListOrderedMap sos(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        Device device = authDevice(param, result);
        if (device == null) {
            return result;
        }
        if (device.getOldId() == null) {
            return fail(result, NOT_BOUND, "设备还未绑定老人");
        }

        String now = DateConst.dateToString(new Date(), DateConst.DATE_MODEL_2);
        String lng = paramValue(param, "lng", device.getLastLng() == null ? "" : device.getLastLng());
        String lat = paramValue(param, "lat", device.getLastLat() == null ? "" : device.getLastLat());

        deviceDao.insertAlarm(device.getOldId(), device.getDeviceId(), lng, lat, now);
        if (lng.length() > 0 && lat.length() > 0) {
            deviceDao.insertLocation(device.getDeviceId(), device.getOldId(), lng, lat, "sos", now);
        }
        deviceDao.touch(device.getDeviceId(), null, null, now);
        logger.warn("收到 SOS 呼叫：oldId={}, deviceId={}", device.getOldId(), device.getDeviceId());

        result.put("message", "已通知家属");
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    // ==================== 后台管理端 ====================

    /** 设备列表 */
    @ResponseBody
    @PostMapping("/list")
    public ListOrderedMap list(@RequestBody(required = false) ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        // 页面每次刷新时顺手识别"刚掉线"的设备，让人员不合格红点及时出现
        deviceJdbcDao.scanOfflineDevices();
        List<DeviceDto> devices = deviceDao.findDevices();
        List<ListOrderedMap> rows = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (DeviceDto dto : devices) {
            ListOrderedMap row = new ListOrderedMap();
            row.put("deviceId", dto.getDeviceId());
            row.put("deviceNo", dto.getDeviceNo());
            row.put("secret", dto.getSecret());
            row.put("alias", dto.getAlias());
            row.put("model", dto.getModel());
            row.put("appVersion", dto.getAppVersion());
            row.put("battery", dto.getBattery());
            row.put("oldId", dto.getOldId());
            row.put("oldPersonName", dto.getOldPersonName() == null ? "（未绑定）" : dto.getOldPersonName());
            row.put("oldPersonPhone", dto.getOldPersonPhone());
            row.put("bindTime", dto.getBindTime());
            row.put("lastTime", dto.getLastTime());
            row.put("lastLng", dto.getLastLng());
            row.put("lastLat", dto.getLastLat());
            row.put("status", dto.getStatus());
            row.put("online", isOnline(dto.getLastTime(), now) ? 1 : 0);
            row.put("personHandled", dto.getPersonHandled());
            row.put("rateHandled", dto.getRateHandled());
            row.put("preHandled", dto.getPreHandled());
            rows.add(row);
        }
        result.put("devices", rows);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 生成绑定码（管理员给老人/家属用）
     * 入参：oldId、expireDays（可选，默认 1 天）
     */
    @ResponseBody
    @PostMapping("/createBindCode")
    public ListOrderedMap createBindCode(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String oldIdStr = paramValue(param, "oldId", "");
        if (!ReturnCodeUtil.isNumeric(oldIdStr) || oldIdStr.length() == 0) {
            return fail(result, ReturnCodeUtil.falseCode, "请选择要绑定的老人");
        }
        int oldId = Integer.parseInt(oldIdStr);
        OldPerson oldPerson = oldPersonDao.getOldPersonById(oldId);
        if (oldPerson == null) {
            return fail(result, ReturnCodeUtil.falseCode, "老人不存在");
        }

        int expireDays = toInt(paramValue(param, "expireDays", "1"), 1, 30);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, expireDays);

        BindCode bindCode = new BindCode();
        bindCode.setOldId(oldId);
        bindCode.setCreateUser(paramValue(param, "createUser", "管理员"));
        bindCode.setExpireTime(DateConst.dateToString(calendar.getTime(), DateConst.DATE_MODEL_2));
        bindCode.setCode(deviceJdbcDao.generateBindCode());

        Integer id = deviceDao.saveBindCode(bindCode);
        if (id == null) {
            return fail(result, ReturnCodeUtil.falseCode, "生成绑定码失败");
        }

        result.put("code", bindCode.getCode());
        result.put("oldId", oldId);
        result.put("oldPersonName", oldPerson.getName());
        result.put("expireTime", bindCode.getExpireTime());
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /** 某老人的绑定码列表 */
    @ResponseBody
    @PostMapping("/bindCodeList")
    public ListOrderedMap bindCodeList(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String oldIdStr = paramValue(param, "oldId", "");
        List<BindCode> codes;
        if (ReturnCodeUtil.isNumeric(oldIdStr) && oldIdStr.length() > 0) {
            codes = deviceDao.findBindCodesByOldId(Integer.parseInt(oldIdStr));
        } else {
            codes = new ArrayList<>();
        }
        List<ListOrderedMap> rows = new ArrayList<>();
        for (BindCode code : codes) {
            ListOrderedMap row = new ListOrderedMap();
            row.put("id", code.getId());
            row.put("code", code.getCode());
            row.put("expireTime", code.getExpireTime());
            row.put("used", code.getUsed());
            row.put("usedTime", code.getUsedTime());
            row.put("usedDeviceId", code.getUsedDeviceId());
            rows.add(row);
        }
        result.put("bindCodes", rows);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /** 作废未使用的绑定码 */
    @ResponseBody
    @PostMapping("/deleteBindCode")
    public ListOrderedMap deleteBindCode(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String idStr = paramValue(param, "id", "");
        if (!ReturnCodeUtil.isNumeric(idStr) || idStr.length() == 0) {
            return fail(result, ReturnCodeUtil.falseCode, "参数不正确");
        }
        int rows = deviceDao.deleteUnusedBindCode(Integer.parseInt(idStr));
        if (rows == 1) {
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
            return result;
        }
        return fail(result, ReturnCodeUtil.falseCode, "绑定码已被使用或不存在");
    }

    /**
     * 新建设备（后台登记一块新手表）
     * 入参：alias、model、oldId（可选）
     */
    @ResponseBody
    @PostMapping("/create")
    public ListOrderedMap create(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String oldIdStr = paramValue(param, "oldId", "");
        Integer oldId = null;
        if (ReturnCodeUtil.isNumeric(oldIdStr) && oldIdStr.length() > 0) {
            oldId = Integer.parseInt(oldIdStr);
            if (oldPersonDao.getOldPersonById(oldId) == null) {
                return fail(result, ReturnCodeUtil.falseCode, "老人不存在");
            }
        }

        Device device = new Device();
        device.setDeviceNo(deviceJdbcDao.generateDeviceNo());
        device.setSecret(deviceJdbcDao.generateSecret());
        device.setAlias(paramValue(param, "alias", "新手表"));
        device.setModel(paramValue(param, "model", "HarmonyOS-Wear"));
        device.setOldId(oldId);
        device.setStatus(1);

        Integer deviceId = deviceDao.save(device);
        if (deviceId == null) {
            return fail(result, ReturnCodeUtil.falseCode, "新建设备失败");
        }
        if (oldId != null) {
            oldPersonDao.updateOldPersonDeviceId(oldId, deviceId);
            deviceDao.saveWatchProfile(deviceId, DEFAULT_HRF, DEFAULT_HRL, DEFAULT_BPF, DEFAULT_BPL,
                    DateConst.dateToString(new Date(), DateConst.DATE_MODEL_5));
        }

        result.put("deviceId", deviceId);
        result.put("deviceNo", device.getDeviceNo());
        result.put("secret", device.getSecret());
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /** 重置设备密钥（示例代码里会明文返回，正式环境应改为让设备重新绑定） */
    @ResponseBody
    @PostMapping("/resetSecret")
    public ListOrderedMap resetSecret(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String deviceIdStr = paramValue(param, "deviceId", "");
        if (!ReturnCodeUtil.isNumeric(deviceIdStr) || deviceIdStr.length() == 0) {
            return fail(result, ReturnCodeUtil.falseCode, "参数不正确");
        }
        int deviceId = Integer.parseInt(deviceIdStr);
        if (deviceDao.findById(deviceId) == null) {
            return fail(result, ReturnCodeUtil.falseCode, "设备不存在");
        }
        String secret = deviceJdbcDao.generateSecret();
        deviceDao.updateSecret(deviceId, secret);
        result.put("secret", secret);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /** 解除绑定 */
    @ResponseBody
    @PostMapping("/unbind")
    public ListOrderedMap unbind(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String deviceIdStr = paramValue(param, "deviceId", "");
        if (!ReturnCodeUtil.isNumeric(deviceIdStr) || deviceIdStr.length() == 0) {
            return fail(result, ReturnCodeUtil.falseCode, "参数不正确");
        }
        int rows = deviceDao.unbind(Integer.parseInt(deviceIdStr));
        if (rows >= 1) {
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
            return result;
        }
        return fail(result, ReturnCodeUtil.falseCode, "解绑失败");
    }

    /** 删除设备 */
    @ResponseBody
    @PostMapping("/delete")
    public ListOrderedMap delete(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String deviceIdStr = paramValue(param, "deviceId", "");
        if (!ReturnCodeUtil.isNumeric(deviceIdStr) || deviceIdStr.length() == 0) {
            return fail(result, ReturnCodeUtil.falseCode, "参数不正确");
        }
        int rows = deviceDao.delete(Integer.parseInt(deviceIdStr));
        if (rows == 1) {
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
            return result;
        }
        return fail(result, ReturnCodeUtil.falseCode, "删除失败");
    }

    /**
     * 告警处置：把某老人最近的告警标记为已处理（1->0）
     * 入参：oldId
     */
    @ResponseBody
    @PostMapping("/handleAlarm")
    public ListOrderedMap handleAlarm(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String oldIdStr = paramValue(param, "oldId", "");
        if (!ReturnCodeUtil.isNumeric(oldIdStr) || oldIdStr.length() == 0) {
            return fail(result, ReturnCodeUtil.falseCode, "参数不正确");
        }
        int rows = deviceDao.updateLatestAlarmStatus(Integer.parseInt(oldIdStr), "0");
        if (rows >= 1) {
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
            return result;
        }
        return fail(result, ReturnCodeUtil.falseCode, "没有待处理的告警");
    }

    // ==================== 异常（红点）与处置 ====================

    /**
     * 各设备未处理的异常数量，用于列表上的红点小数字
     * 返回：[{deviceId, deviceNo, alias, personCount, rateCount, preCount}]
     */
    @ResponseBody
    @PostMapping("/alertCounts")
    public ListOrderedMap alertCounts(@RequestBody(required = false) ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        deviceJdbcDao.scanOfflineDevices();
        List<Map<String, Object>> rows = deviceDao.countPendingAlerts();
        int person = 0;
        int rate = 0;
        int pre = 0;
        List<ListOrderedMap> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ListOrderedMap item = new ListOrderedMap();
            item.put("deviceId", row.get("deviceId"));
            item.put("deviceNo", row.get("deviceNo"));
            item.put("alias", row.get("alias"));
            item.put("personCount", row.get("personCount"));
            item.put("rateCount", row.get("rateCount"));
            item.put("preCount", row.get("preCount"));
            items.add(item);
            person += toInt(row.get("personCount"));
            rate += toInt(row.get("rateCount"));
            pre += toInt(row.get("preCount"));
        }
        result.put("items", items);
        result.put("personTotal", person);
        result.put("rateTotal", rate);
        result.put("preTotal", pre);
        result.put("total", person + rate + pre);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 查看某台设备某类未处理异常的原因（红点点进去看这个）
     * 入参：deviceId、kind = person / rate / pre
     */
    @ResponseBody
    @PostMapping("/alertDetail")
    public ListOrderedMap alertDetail(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String deviceIdStr = paramValue(param, "deviceId", "");
        if (!ReturnCodeUtil.isNumeric(deviceIdStr) || deviceIdStr.length() == 0) {
            return fail(result, ReturnCodeUtil.falseCode, "参数不正确");
        }
        int deviceId = Integer.parseInt(deviceIdStr);
        String kind = paramValue(param, "kind", "person").toLowerCase();

        // 先做一次设备在线状态扫描：掉线/恢复在线会改变"人员不合格"红点
        deviceJdbcDao.scanOfflineDevices();

        Device device = deviceDao.findById(deviceId);
        if (device == null) {
            return fail(result, ReturnCodeUtil.falseCode, "设备不存在");
        }
        result.put("deviceId", deviceId);
        result.put("deviceNo", device.getDeviceNo());
        result.put("alias", device.getAlias());
        result.put("oldPersonName", device.getOldId() == null ? "（未绑定）" : oldPersonName(device.getOldId()));
        result.put("personHandled", device.getPersonHandled());
        result.put("rateHandled", device.getRateHandled());
        result.put("preHandled", device.getPreHandled());

        Map<String, Object> alert = deviceDao.findPendingAlert(deviceId, kind);
        if (alert == null) {
            result.put("hasAlert", 0);
            result.put("message", "该设备当前没有未处理的「" + kindText(kind) + "」问题");
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
            return result;
        }
        result.put("hasAlert", 1);
        result.put("kind", alert.get("kind"));
        result.put("kindText", alert.get("kindText"));
        result.put("reason", alert.get("reason"));
        result.put("detail", alert.get("detail"));
        result.put("value", alert.get("value"));
        result.put("threshold", alert.get("threshold"));
        result.put("occurTime", alert.get("occurTime"));
        result.put("recordId", alert.get("recordId"));
        result.put("advice", adviceOf(kind));
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 处置（消除红点）：把某台设备某类异常标记为已处理
     * 入参：deviceId、kind = person / rate / pre、recordId（可选，rate/pre 用）
     */
    @ResponseBody
    @PostMapping("/resolveAlert")
    public ListOrderedMap resolveAlert(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String deviceIdStr = paramValue(param, "deviceId", "");
        if (!ReturnCodeUtil.isNumeric(deviceIdStr) || deviceIdStr.length() == 0) {
            return fail(result, ReturnCodeUtil.falseCode, "参数不正确");
        }
        int deviceId = Integer.parseInt(deviceIdStr);
        String kind = paramValue(param, "kind", "person").toLowerCase();
        String recordIdStr = paramValue(param, "recordId", "");
        Integer recordId = ReturnCodeUtil.isNumeric(recordIdStr) && recordIdStr.length() > 0
                ? Integer.parseInt(recordIdStr) : null;

        if (deviceDao.findById(deviceId) == null) {
            return fail(result, ReturnCodeUtil.falseCode, "设备不存在");
        }

        String before = null;
        Map<String, Object> pending = deviceDao.findPendingAlert(deviceId, kind);
        if (pending != null) {
            before = String.valueOf(pending.get("reason"));
        }

        int rows = 0;
        if ("rate".equals(kind) || "pre".equals(kind)) {
            rows = deviceDao.resolveRatepreAlert(deviceId, kind, recordId);
            // 该类异常都处置完了，就把设备上的标记也置为已处理，红点彻底消失
            ((DeviceJdbcDao) deviceDao).clearDeviceFlagIfNoAlert(deviceId, kind);
        } else if ("person".equals(kind)) {
            rows = deviceDao.resolveDeviceAlert(deviceId, kind);
        } else {
            return fail(result, ReturnCodeUtil.falseCode, "未知的异常类型：" + kind);
        }

        if (rows < 0) {
            return fail(result, ReturnCodeUtil.falseCode, "处置失败");
        }
        logger.info("异常处置：deviceId={}, kind={}, reason={}，已标记为已处理", deviceId, kind, before);
        result.put("handledReason", before == null ? "" : before);
        result.put("message", "已处置，红点已消除");
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /** 处置建议：告诉使用者这个问题该怎么处理 */
    private String adviceOf(String kind) {
        if ("person".equals(kind)) {
            return "① 未绑定：点「绑定码」生成绑定码，在手表上输入完成绑定；"
                    + "② 离线/从未上报：确认手表已开机、连上同一 Wi-Fi，服务器地址填 http://电脑IP:8795；"
                    + "③ 电量低：联系家属提醒充电；"
                    + "④ 未配置阈值：到「基础管理-手表信息」补心率/血压上下限。处理完点「标记已处理」消除红点。";
        }
        if ("rate".equals(kind)) {
            return "① 先联系家属确认老人身体状况；② 持续异常建议就医；"
                    + "③ 若是阈值设置不合理（老人基础心率偏高/偏低），到「手表信息」调整心率上下限；"
                    + "④ 确认无误后点「标记已处理」消除红点。";
        }
        return "① 先联系家属确认老人身体状况；② 持续偏高/偏低建议就医；"
                + "③ 若血压计或手表测量有偏差，可重新测量一次；"
                + "④ 确认无误后点「标记已处理」消除红点。";
    }

    private String kindText(String kind) {
        if ("rate".equals(kind)) {
            return "心率不合格";
        }
        if ("pre".equals(kind)) {
            return "血压不合格";
        }
        return "人员不合格";
    }

    private String oldPersonName(Integer oldId) {
        OldPerson oldPerson = oldPersonDao.getOldPersonById(oldId);
        return oldPerson == null ? "" : oldPerson.getName();
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

    // ==================== 内部方法 ====================

    /** 设备鉴权：失败时把错误写进 result 并返回 null */
    private Device authDevice(ListOrderedMap param, ListOrderedMap result) {
        String deviceNo = paramValue(param, "deviceNo", "").toUpperCase();
        String secret = paramValue(param, "secret", "");
        Device device = deviceDao.findByDeviceNo(deviceNo);
        if (device == null || !secret.equals(device.getSecret())) {
            fail(result, AUTH_FAIL, "设备号或密钥不正确");
            return null;
        }
        if (device.getStatus() != null && device.getStatus() == 0) {
            fail(result, AUTH_FAIL, "设备已停用");
            return null;
        }
        return device;
    }

    /** 写入心率血压记录（沿用原有 ratepre 表） */
    private void insertRatepre(Integer deviceId, Integer oldId, String rateState, String preState,
                               String beTime, int hr, int bp) {
        jdbcTemplate.update(
                "insert into ratepre(device_id, old_id, rate_state, pre_state, create_time, hr, bp)"
                        + " values(?,?,?,?,?,?,?)",
                deviceId, oldId, rateState, preState, beTime, hr, bp);
    }

    private ListOrderedMap fail(ListOrderedMap result, String code, String message) {
        result.put(ReturnCodeUtil.returnCode, code);
        result.put("message", message);
        return result;
    }

    private boolean isExpired(String expireTime) {
        if (expireTime == null || expireTime.trim().length() == 0) {
            return false;
        }
        Date expire = DateConst.toDate(expireTime.trim(), DateConst.DATE_MODEL_2);
        return expire != null && expire.getTime() <= System.currentTimeMillis();
    }

    /** 设备是否在线：默认 60 秒内有心跳算在线 */
    private boolean isOnline(String lastTime, long now) {
        if (lastTime == null || lastTime.trim().length() == 0) {
            return false;
        }
        Date date = DateConst.toDate(lastTime.trim(), DateConst.DATE_MODEL_2);
        return date != null && (now - date.getTime()) <= 60L * 1000;
    }

    private int toInt(String value, int min, int max) {
        if (value == null || value.trim().length() == 0) {
            return 0;
        }
        try {
            int result = Integer.parseInt(value.trim());
            if (result < min || result > max) {
                return 0;
            }
            return result;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String paramValue(ListOrderedMap param, String key, String defaultValue) {
        if (param == null) {
            return defaultValue;
        }
        Object value = param.get(key);
        if (value == null) {
            return defaultValue;
        }
        String str = value.toString().trim();
        return str.length() == 0 ? defaultValue : str;
    }
}
