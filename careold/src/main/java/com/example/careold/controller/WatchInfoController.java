package com.example.careold.controller;

import com.example.careold.common.DateConst;
import com.example.careold.common.ReturnCodeUtil;
import com.example.careold.dao.OldPersonDao;
import com.example.careold.dao.WatchInfoDao;
import com.example.careold.domain.OldPerson;
import com.example.careold.domain.WatchDto;
import com.example.careold.domain.WatchInfo;
import org.apache.commons.collections.map.ListOrderedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/restful/watchInfo")
public class WatchInfoController {

    @Autowired
    WatchInfoDao watchInfoDao;

    @ResponseBody
    @PostMapping("/select")
    public ListOrderedMap select(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String oldName=param.get("oldName").toString();
        List<WatchDto> watchDtos=watchInfoDao.getWatchInfo(oldName);
        result.put("watchDtos",watchDtos);
        return result;
    }

    @ResponseBody
    @PostMapping("/add")
    public ListOrderedMap add(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String deviceId=param.get("deviceId").toString();
        List<WatchDto> watchInfo2 = watchInfoDao.getWatchInfo2(Integer.parseInt(deviceId));
        if(watchInfo2.size()==0){
            String bpf=param.get("bpf").toString();
            String bpl=param.get("bpl").toString();
            String hrf=param.get("hrf").toString();
            String hrl=param.get("hrl").toString();
            WatchInfo watchInfo=new WatchInfo();
            watchInfo.setDeviceId(Integer.parseInt(deviceId));
            watchInfo.setOpTime(DateConst.dateToString(new Date(),DateConst.DATE_MODEL_1));
            watchInfo.setBpf(Integer.parseInt(bpf));
            watchInfo.setBpl(Integer.parseInt(bpl));
            watchInfo.setHrf(Integer.parseInt(hrf));
            watchInfo.setHrl(Integer.parseInt(hrl));
            int rows=watchInfoDao.addWatchInfo(watchInfo);
            if(rows==1){
                result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
                return result;
            }
        }

        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
        return result;
    }

    @ResponseBody
    @PostMapping("/update")
    public ListOrderedMap update(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String deviceId=param.get("deviceId").toString();
        String bpf=param.get("bpf").toString();
        String bpl=param.get("bpl").toString();
        String hrf=param.get("hrf").toString();
        String hrl=param.get("hrl").toString();
        WatchInfo watchInfo=new WatchInfo();
        watchInfo.setDeviceId(Integer.parseInt(deviceId));
        watchInfo.setBpf(Integer.parseInt(bpf));
        watchInfo.setBpl(Integer.parseInt(bpl));
        watchInfo.setHrf(Integer.parseInt(hrf));
        watchInfo.setHrl(Integer.parseInt(hrl));
        int rows=watchInfoDao.updateWatchInfo(watchInfo);
        if(rows==1){
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
            return result;
        }
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
        return result;
    }

    @ResponseBody
    @PostMapping("/delete")
    public ListOrderedMap delete(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String deviceId=param.get("deviceId").toString();

        int rows=watchInfoDao.deleteWatchInfo(deviceId);
        if(rows==1){
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
            return result;
        }
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
        return result;
    }
}
