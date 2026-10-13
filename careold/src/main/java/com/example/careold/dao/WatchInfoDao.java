package com.example.careold.dao;

import com.example.careold.domain.WatchDto;
import com.example.careold.domain.WatchInfo;

import java.util.List;

public interface WatchInfoDao {

    List<WatchDto> getWatchInfo(String oldName);

    List<WatchDto> getWatchInfo2(int deviceId);

    int addWatchInfo(WatchInfo watchInfo);

    int updateWatchInfo(WatchInfo watchInfo);

    int deleteWatchInfo(String deviceId);
}
