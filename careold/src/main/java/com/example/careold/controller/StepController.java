package com.example.careold.controller;

import com.example.careold.dao.RatepreDao;
import com.example.careold.dao.StepDao;
import com.example.careold.domain.RatepreDto;
import com.example.careold.domain.StepDto;
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
import java.util.List;

@Controller
@RequestMapping("/restful/step")
public class StepController {

    @Autowired
    StepDao stepDao;

    @Autowired
    RatepreDao ratepreDao;

    private static final Logger LOG = LoggerFactory.getLogger(RatepreController.class);

    //睡眠步数查询
    @ResponseBody
    @PostMapping("/selectStep")
    public ListOrderedMap selectRatepre(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String name=param.get("name").toString();
        String timeFirst=param.get("timeFirst").toString();
        String timeLast=param.get("timeLast").toString();
        LOG.info(name+"----"+timeFirst+"----"+timeLast);
        List<StepDto> stepDtos=stepDao.getStepSleep(name,timeFirst,timeLast);
        result.put("stepDtos",stepDtos);
        return result;
    }

    @ResponseBody
    @PostMapping("/selectStepFamily")
    public ListOrderedMap selectStepFamily(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String oldId=param.get("oldId").toString();
        List<StepDto> stepDtos=stepDao.getStepSleepFamily(Integer.parseInt(oldId));
        result.put("stepDtos",stepDtos);
        return result;
    }


    @ResponseBody
    @PostMapping("/selectStepDetail")
    public ListOrderedMap selectStepDetail(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String oldId=param.get("oldId").toString();
        List<StepDto> stepDtos=stepDao.getStepSleepDetail(Integer.parseInt(oldId));
        ArrayList sleep=new ArrayList();
        ArrayList createTime=new ArrayList();
        for(int i=0;i<stepDtos.size();i++){
            sleep.add(stepDtos.get(i).getSleep());
            createTime.add(stepDtos.get(i).getCreateTime().substring(8,10));
        }
        result.put("stepDtos",stepDtos);
        result.put("sleep",sleep);
        result.put("createTime",createTime);
        return result;
    }

    @ResponseBody
    @PostMapping("/selectStepCount")
    public ListOrderedMap selectStepCount(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        List<String> deviceIdList=stepDao.getSleepBadCount();
        result.put("deviceIdList",deviceIdList);
        return result;
    }

    @ResponseBody
    @PostMapping("/getAllCount")
    public ListOrderedMap getAllCount(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        List<String> rates=ratepreDao.getRateCount();
        List<String> pres=ratepreDao.getPreCount();
        List<String> deviceIdList=stepDao.getSleepBadCount();
        String allCount=rates.size()+pres.size()+deviceIdList.size()+"";
        result.put("allCount",allCount);
        return result;
    }

}
