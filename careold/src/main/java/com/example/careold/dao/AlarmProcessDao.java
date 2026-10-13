package com.example.careold.dao;

import com.example.careold.domain.AlarmProcess;

import java.util.List;

public interface AlarmProcessDao {
    List<AlarmProcess> getAlarmProcess(int id);
}
