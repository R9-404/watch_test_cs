package com.example.careold.controller;

import com.example.careold.dao.AlarmProcessDao;
import com.example.careold.domain.AlarmProcess;
import org.apache.commons.collections.map.ListOrderedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/restful/alarmProcess")
public class AlarmProcessController {

    @Autowired
    AlarmProcessDao alarmProcessDao;

    @ResponseBody
    @PostMapping("/select")
    public ListOrderedMap select(@RequestBody ListOrderedMap params){
        System.out.println("进入");
        ListOrderedMap result=new ListOrderedMap();
        String id=(String)params.get("id");
        List<AlarmProcess> alarmProcesses= alarmProcessDao.getAlarmProcess(Integer.parseInt(id));
        result.put("alarmProcesses",alarmProcesses);
        return result;
    }
}
