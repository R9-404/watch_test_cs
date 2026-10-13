package com.example.careold.dao;

import com.example.careold.domain.RatepreDto;

import java.util.List;
import java.util.Map;

public interface RatepreDao {

    List<RatepreDto> getRatepre(String name,String timeFirst,String timeLast);

    List<RatepreDto> getRatepreFamily(int old_id, String timeFirst, String timeLast);

    List<RatepreDto> getRatepreDetial(int oldId);

    List<String> getRateCount();

    List<String> getPreCount();

    /** 未处置的心率/血压异常条数（红点小数字）：kind = rate / pre */
    int countPendingAlert(String kind);

    /** 查一条记录（含处置状态），用于红点详情弹窗；找不到返回 null */
    Map<String, Object> findAlertRecord(int recordId);

    /** 按老人查最近一条未处理的异常记录（kind = rate / pre，为空表示心率血压任一）；找不到返回 null */
    Map<String, Object> findLatestPendingByOldId(int oldId, String kind);

    /** 处置一条异常记录：kind = rate / pre，处置后异常标记归0、已处理标记置1 */
    int resolveAlert(int recordId, String kind);
}
