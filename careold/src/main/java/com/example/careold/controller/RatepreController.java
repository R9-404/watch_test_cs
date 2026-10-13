package com.example.careold.controller;

import com.example.careold.common.ReturnCodeUtil;
import com.example.careold.dao.RatepreDao;
import com.example.careold.domain.RatepreDto;
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
@RequestMapping("/restful/ratepre")
public class RatepreController {

    @Autowired
    RatepreDao ratepreDao;

    private static final Logger LOG = LoggerFactory.getLogger(RatepreController.class);

    /** 异常（红点）处置接口 —— 供心率血压页面与设备管理页面跳转后使用 */

    /**
     * 未处置异常数量：{ rateTotal: 心率异常条数, preTotal: 血压异常条数 }
     */
    @ResponseBody
    @PostMapping("/alertCount")
    public ListOrderedMap alertCount(@RequestBody(required = false) ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        int rate=ratepreDao.countPendingAlert("rate");
        int pre=ratepreDao.countPendingAlert("pre");
        result.put("rateTotal",rate);
        result.put("preTotal",pre);
        result.put("total",rate+pre);
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 查看一条异常记录的原因（红点点进去看这个）
     * 入参：recordId
     */
    @ResponseBody
    @PostMapping("/alertDetail")
    public ListOrderedMap alertDetail(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String recordId=param.get("recordId")==null?"":param.get("recordId").toString();
        String oldIdStr=param.get("oldId")==null?"":param.get("oldId").toString();
        String kind=param.get("kind")==null?"":param.get("kind").toString().toLowerCase();
        java.util.Map<String,Object> row=null;
        if(recordId.length()>0 && ReturnCodeUtil.isNumeric(recordId)){
            row=ratepreDao.findAlertRecord(Integer.parseInt(recordId));
        }else if(oldIdStr.length()>0 && ReturnCodeUtil.isNumeric(oldIdStr)){
            // 心率血压页面点"查看原因"时只带 name，这里按老人找最近一条未处理异常
            row=ratepreDao.findLatestPendingByOldId(Integer.parseInt(oldIdStr),kind);
        }else{
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
            result.put("message","参数不正确");
            return result;
        }
        if(row==null){
            result.put("hasAlert",0);
            result.put("message","该老人当前没有待处理的心率/血压异常");
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
            return result;
        }
        int hr=toInt(row.get("hr"));
        int bp=toInt(row.get("bp"));
        // rateDone/preDone 由 SQL 依据"处理时间"算好：该记录已经被处理过
        boolean rateHandled="1".equals(String.valueOf(row.get("rateDone")));
        boolean preHandled="1".equals(String.valueOf(row.get("preDone")));
        boolean rateBad="1".equals(String.valueOf(row.get("rateState")));
        boolean preBad="1".equals(String.valueOf(row.get("preState")));

        List<String> reasons=new ArrayList<>();
        List<String> details=new ArrayList<>();
        if(rateBad && !rateHandled){
            reasons.add(hr > 110 ? "心率偏高" : "心率偏低");
            details.add("心率正常范围 55-110 次/分，本次测量 "+hr+" 次/分。");
        }
        if(preBad && !preHandled){
            reasons.add(bp > 140 ? "血压偏高" : "血压偏低");
            details.add("血压正常范围 90-140 mmHg，本次测量 "+bp+" mmHg。");
        }
        if(reasons.isEmpty()){
            result.put("hasAlert",0);
            result.put("message","该记录已处理，没有待处理的红点");
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
            return result;
        }

        result.put("hasAlert",1);
        result.put("recordId",row.get("id"));
        result.put("deviceId",row.get("deviceId"));
        result.put("oldId",row.get("oldId"));
        result.put("oldPersonName",row.get("oldPersonName"));
        result.put("hr",hr);
        result.put("bp",bp);
        result.put("rateBad",rateBad && !rateHandled ? 1 : 0);
        result.put("preBad",preBad && !preHandled ? 1 : 0);
        result.put("occurTime",row.get("createTime"));
        result.put("reason",join(reasons));
        result.put("detail",join(details));
        result.put("advice","① 先联系家属确认老人身体状况；② 持续异常建议就医；"
                + "③ 若是阈值设置不合理，到「基础管理-手表信息」调整心率/血压上下限；"
                + "④ 确认无误后点「标记已处理」消除红点。");
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 处置异常（消除红点）
     * 入参：recordId、kind = rate（心率）/ pre（血压）/ 为空表示两类都处置
     */
    @ResponseBody
    @PostMapping("/resolveAlert")
    public ListOrderedMap resolveAlert(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String recordId=param.get("recordId")==null?"":param.get("recordId").toString();
        String kind=param.get("kind")==null?"":param.get("kind").toString().toLowerCase();
        if(recordId.length()==0 || !ReturnCodeUtil.isNumeric(recordId)){
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
            result.put("message","参数不正确");
            return result;
        }
        if(ratepreDao.findAlertRecord(Integer.parseInt(recordId))==null){
            result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
            result.put("message","记录不存在");
            return result;
        }
        int rows=0;
        if("rate".equals(kind) || "pre".equals(kind)){
            rows=ratepreDao.resolveAlert(Integer.parseInt(recordId),kind);
        }else{
            rows=ratepreDao.resolveAlert(Integer.parseInt(recordId),"rate");
            rows+=ratepreDao.resolveAlert(Integer.parseInt(recordId),"pre");
        }
        LOG.info("心率血压异常处置：recordId={}, kind={}", recordId, kind.length()==0?"全部":kind);
        result.put("message","已处置，红点已消除");
        result.put(ReturnCodeUtil.returnCode,rows>=1?ReturnCodeUtil.successCode:ReturnCodeUtil.falseCode);
        return result;
    }

    private String join(List<String> items){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<items.size();i++){
            if(i>0){ sb.append("；"); }
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private int toInt(Object value){
        if(value==null){ return 0; }
        try{
            return Integer.parseInt(value.toString().trim());
        }catch(NumberFormatException e){
            return 0;
        }
    }

    //心率血压查询
    @ResponseBody
    @PostMapping("/selectRatepre")
    public ListOrderedMap selectRatepre(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String name=param.get("name").toString();
        String timeFirst=param.get("timeFirst").toString();
        String timeLast=param.get("timeLast").toString();
        LOG.info(name+"----"+timeFirst+"----"+timeLast);
        List<RatepreDto> ratepreDtoList=ratepreDao.getRatepre(name,timeFirst,timeLast);
        result.put("ratepreDtoList",ratepreDtoList);
        return result;
    }

    @ResponseBody
    @PostMapping("/selectRatepreFamily")
    public ListOrderedMap selectRatepreFamily(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String oldId=param.get("oldId").toString();
        String timeFirst=null;
        String timeLast=null;
        LOG.info(oldId+"----"+timeFirst+"----"+timeLast);
        List<RatepreDto> ratepreDtoList=ratepreDao.getRatepreFamily(Integer.parseInt(oldId),timeFirst,timeLast);
        result.put("ratepreDtoList",ratepreDtoList);
        return result;
    }

    //查询心率血压详细情况
    @ResponseBody
    @PostMapping("/selectRatepreDetail")
    public ListOrderedMap selectRatepreDetail(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        String oldId=param.get("oldId").toString();
        List<RatepreDto> ratepreDtoListDetial=ratepreDao.getRatepreDetial(Integer.parseInt(oldId));
        ArrayList hrs=new ArrayList();
        ArrayList bps=new ArrayList();
        ArrayList createTimes=new ArrayList();
        for(int i=0;i<ratepreDtoListDetial.size();i++){
            hrs.add(ratepreDtoListDetial.get(i).getHr());
            bps.add(ratepreDtoListDetial.get(i).getBp());
            createTimes.add(ratepreDtoListDetial.get(i).getCreateTime().substring(14,16));
        }
        result.put("time",ratepreDtoListDetial.get(0).getCreateTime().substring(0,13));
        result.put("ratepreDtoListDetial",ratepreDtoListDetial);
        result.put("hrs",hrs);
        result.put("bps",bps);
        result.put("createTimes",createTimes);
        return result;
    }

    @ResponseBody
    @PostMapping("/selectRatepreCount")
    public ListOrderedMap selectRatepreCount(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();
        List<String> rates=ratepreDao.getRateCount();
        List<String> pres=ratepreDao.getPreCount();
        result.put("rates",rates);
        result.put("pres",pres);
        return result;
    }
}
