package com.example.careold.controller;


import com.example.careold.common.DateConst;
import com.example.careold.common.ReturnCodeUtil;
import com.example.careold.dao.OldPersonDao;
import com.example.careold.dao.InviteCodeDao;
import com.example.careold.dao.PositionDao;
import com.example.careold.dao.WatchInfoDao;
import com.example.careold.domain.OldPerson;
import com.example.careold.domain.PositionDto;
import com.example.careold.domain.WatchDto;
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
@RequestMapping("/restful/oldPerson")
public class OldPersonController {

    @Autowired
    OldPersonDao oldPersonDao;

    @Autowired
    WatchInfoDao watchInfoDao;

    @Autowired
    InviteCodeDao inviteCodeDao;

    //对老人的查询
    @ResponseBody
    @PostMapping("/select")
    public ListOrderedMap select(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String name=param.get("name").toString();
        List<OldPerson> oldPeople=oldPersonDao.getOldPerson(name);
        result.put("oldPeople",oldPeople);
        return result;
    }

    @ResponseBody
    @PostMapping("/add")
    public ListOrderedMap add(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String deviceId=param.get("deviceId").toString();
        List<WatchDto> watchInfo2 = watchInfoDao.getWatchInfo2(Integer.parseInt(deviceId));
        if(watchInfo2.size()==1){
            String name=param.get("name").toString();
            String bothdate=param.get("bothdate").toString();
            String bothnian1=bothdate.substring(0,4);
            String sex=param.get("sex").toString();
            String phone=param.get("phone").toString();
            String room=param.get("room").toString();
            String beTime= DateConst.dateToString(new Date(),DateConst.DATE_MODEL_1);
            String bothnian2=beTime.substring(0,4);
            String illness=param.get("illness").toString();
            String warn=param.get("warn").toString();
            String foodLike=param.get("foodLike").toString();
            String foodHate=param.get("foodHate").toString();
            String drag=param.get("drag").toString();
            String customerId=param.get("customerId").toString();
            OldPerson oldPerson=new OldPerson();
            oldPerson.setName(name);
            oldPerson.setAge(Integer.parseInt(bothnian2)-Integer.parseInt(bothnian1));
            oldPerson.setBeTime(beTime);
            oldPerson.setSex(sex);
            oldPerson.setPhone(phone);
            oldPerson.setRoom(room);
            oldPerson.setIllness(illness);
            oldPerson.setWarn(warn);
            oldPerson.setFoodLike(foodLike);
            oldPerson.setFoodHate(foodHate);
            oldPerson.setDrag(drag);
            oldPerson.setBothdate(bothdate);
            oldPerson.setDeviceId(Integer.parseInt(deviceId));
            oldPerson.setCustomerId(Integer.parseInt(customerId));
            int rows=oldPersonDao.addOldPerson(oldPerson);
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
        String name=param.get("name").toString();
        String bothdate=param.get("bothdate").toString();
        String bothnian1=bothdate.substring(0,4);
        String sex=param.get("sex").toString();
        String phone=param.get("phone").toString();
        String room=param.get("room").toString();
        String beTime= DateConst.dateToString(new Date(),DateConst.DATE_MODEL_1);
        String bothnian2=beTime.substring(0,4);
        String illness=param.get("illness").toString();
        String warn=param.get("warn").toString();
        String foodLike=param.get("foodLike").toString();
        String foodHate=param.get("foodHate").toString();
        String drag=param.get("drag").toString();
        String oldId=param.get("oldId").toString();
        String deviceId=param.get("deviceId").toString();
        String customerId=param.get("customerName").toString();
        if(!ReturnCodeUtil.isNumeric(customerId)){
            customerId=param.get("customerId").toString();
        }
        OldPerson oldPerson=new OldPerson();
        oldPerson.setName(name);
        oldPerson.setAge(Integer.parseInt(bothnian2)-Integer.parseInt(bothnian1));
//        oldPerson.setBeTime(beTime);
        oldPerson.setSex(sex);
        oldPerson.setPhone(phone);
        oldPerson.setRoom(room);
        oldPerson.setIllness(illness);
        oldPerson.setWarn(warn);
        oldPerson.setFoodLike(foodLike);
        oldPerson.setFoodHate(foodHate);
        oldPerson.setDrag(drag);
        oldPerson.setBothdate(bothdate);
        oldPerson.setOldId(Integer.parseInt(oldId));
        oldPerson.setDeviceId(Integer.parseInt(deviceId));
        oldPerson.setCustomerId(Integer.parseInt(customerId));
        int rows=oldPersonDao.updateOldPerson(oldPerson);
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
        String oldId=param.get("oldId").toString();
        // 老人删除后，其名下未使用的邀请码一并作废，避免出现无主邀请码
        inviteCodeDao.deleteByOldId(Integer.parseInt(oldId));
        int rows=oldPersonDao.deleteOldPerson(Integer.parseInt(oldId));
        if(rows==1){
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
            return result;
        }
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
        return result;
    }

}
