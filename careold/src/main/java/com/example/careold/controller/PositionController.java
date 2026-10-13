package com.example.careold.controller;


import com.example.careold.dao.PositionDao;
import com.example.careold.dao.SaidDao;
import com.example.careold.domain.PositionDto;
import com.example.careold.domain.Said;
import org.apache.commons.collections.map.ListOrderedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/restful/position")
public class PositionController {

    @Autowired
    PositionDao positionDao;

    @ResponseBody
    @PostMapping("/select")
    public ListOrderedMap select(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String name=param.get("name").toString();
        List<PositionDto> positionDtos=positionDao.getPosition(name);
        result.put("positionDtos",positionDtos);
        return result;
    }

    @ResponseBody
    @PostMapping("/selectFamily")
    public ListOrderedMap selectFamily(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String oldId=param.get("oldId").toString();
        List<PositionDto> positionDtos=positionDao.getPositionFamily(Integer.parseInt(oldId));
        result.put("positionDtos",positionDtos);
        return result;

    }

}
