package com.example.careold.dao;

import com.example.careold.domain.BindCode;
import com.example.careold.domain.Device;
import com.example.careold.domain.DeviceDto;

import java.util.List;

public interface DeviceDao {

    /** 按设备号查询设备（不存在返回 null） */
    Device findByDeviceNo(String deviceNo);

    /** 按主键查询设备（不存在返回 null） */
    Device findById(Integer deviceId);

    /** 设备列表（含绑定老人姓名），只返回启用中的设备 */
    List<DeviceDto> findDevices();

    /** 新增设备，返回自增主键（失败返回 null） */
    Integer save(Device device);

    /** 更新设备档案（别名/型号/绑定/状态等） */
    int update(Device device);

    /** 更新设备密钥 */
    int updateSecret(Integer deviceId, String secret);

    /** 心跳：更新最后在线时间、电量、版本 */
    int touch(Integer deviceId, Integer battery, String appVersion, String lastTime);

    /** 记录最近位置（冗余在 device 表，便于列表直接展示） */
    int updateLastPosition(Integer deviceId, String lng, String lat);

    /** 写入/更新设备的正常阈值档案（watch_info：心率、血压上下限） */
    int saveWatchProfile(Integer deviceId, Integer hrf, Integer hrl, Integer bpf, Integer bpl, String opTime);

    /** 解除绑定 */
    int unbind(Integer deviceId);

    /** 删除设备 */
    int delete(Integer deviceId);

    /** 该老人是否已有绑定的设备 */
    int countByOldId(Integer oldId);

    /** 新增绑定码，返回自增主键（失败返回 null） */
    Integer saveBindCode(BindCode bindCode);

    /** 按绑定码查询（不存在返回 null） */
    BindCode findBindCode(String code);

    /** 绑定码置为已使用（原子，只能成功一次） */
    int markBindCodeUsed(Integer id, Integer deviceId, String usedTime);

    /** 某老人的绑定码列表 */
    List<BindCode> findBindCodesByOldId(Integer oldId);

    /** 作废未使用的绑定码 */
    int deleteUnusedBindCode(Integer id);

    /** 记录位置流水 */
    int insertLocation(Integer deviceId, Integer oldId, String lng, String lat, String source, String beTime);

    /** 记录告警（复用 alarm_precess，status=1 表示摔倒/告警） */
    int insertAlarm(Integer oldId, Integer deviceId, String lng, String lat, String beTime);

    /** 更新指定老人最近一条告警的处置状态 */
    int updateLatestAlarmStatus(Integer oldId, String status);

    /** 写入/覆盖某设备某天的睡眠步数（watch_day_info，每天保留一条） */
    int saveDayInfo(Integer deviceId, int step, int sleep, String sleepState, String day);

    // ==================== 异常（红点）查询与处置 ====================

    /**
     * 读取设备最近一次"未处理的异常"，用于红点与处置弹窗。
     *
     * @param deviceId 设备ID
     * @param kind     person / rate / pre
     * @return 不存在返回 null；key：kind、reason、detail、value、threshold、occurTime、recordId
     */
    java.util.Map<String, Object> findPendingAlert(Integer deviceId, String kind);

    /** 设备层面的异常汇总（用于设备列表红点小数字） */
    List<java.util.Map<String, Object>> countPendingAlerts();

    /** 把设备层面的某类异常标记为已处理 */
    int resolveDeviceAlert(Integer deviceId, String kind);

    /** 处置心率/血压异常：未处理标记置1，异常标记归0；recordId 为空表示处置该设备最新的异常记录 */
    int resolveRatepreAlert(Integer deviceId, String kind, Integer recordId);
}
