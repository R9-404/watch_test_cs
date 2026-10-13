package com.example.careold.controller;

import com.example.careold.common.MD5Util;
import com.example.careold.common.ReturnCodeUtil;
import com.example.careold.dao.FamilyDao;
import com.example.careold.dao.UserDao;
import com.example.careold.dao.WatchInfoDao;
import com.example.careold.domain.*;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import java.util.List;

@Controller
@RequestMapping("/restful/userLogin")
public class LoginController {

    @Autowired
    FamilyDao familyDao;

    @Autowired
    UserDao userDao;

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);


    //登陆
    @ResponseBody
    @PostMapping("/login")
    public ListOrderedMap select(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();

        String phone=param.get("phone").toString();
        String password=param.get("password").toString();
        List<Users> users = userDao.findById(null);
        for(int i=0;i<users.size();i++){
            if(users.get(i).getUserPhone().equals(phone)){
                if(users.get(i).getPassword().equals(password)){
                    Users users1=userDao.getUsersByPhone(phone);
                    result.put("users1",users1);
                    result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
                    return result;
                }else {
                    result.put(ReturnCodeUtil.returnCode,"密码不正确");
                    return result;
                }
            }
        }
        List<Family> families=familyDao.findById(null);
        for(int i=0;i<families.size();i++){
            if(families.get(i).getFamilyPhone().equals(phone)){
                if(families.get(i).getFamilyPassword().equals(password)){
                    FamilyDto family=familyDao.findFamilyByPhone(phone);
                    // 该家属账号没有关联任何老人（例如老人被删除、或账号是历史遗留数据）
                    // 此时 family 为 null，直接返回提示，避免前端拿到 null 后报错
                    if(family==null){
                        result.put(ReturnCodeUtil.returnCode,"该家属账号尚未关联老人，请联系管理员");
                        return result;
                    }
                    result.put("family",family);
                    result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.successCode);
                    return result;
                }else {
                    result.put(ReturnCodeUtil.returnCode,"密码不正确");
                    return result;
                }
            }
        }
        result.put(ReturnCodeUtil.returnCode,ReturnCodeUtil.falseCode);
        return result;
    }

    @ResponseBody
    @PostMapping("/loginTwo")
    public ListOrderedMap selectTwo(@RequestBody ListOrderedMap param){
        ListOrderedMap result=new ListOrderedMap();

        String username=param.get("username").toString();
        String password=param.get("password").toString();
//        List<Users> users = userDao.findById(null);

        Subject sub = SecurityUtils.getSubject();

        UsernamePasswordToken token = new UsernamePasswordToken(username, MD5Util.MD5(password));
        try {
            sub.login(token);
        } catch (UnknownAccountException e) {
            logger.error("对用户[{}]进行登录验证,验证未通过,用户不存在", username);
            token.clear();
            return result;
        } catch (LockedAccountException lae) {
            logger.error("对用户[{}]进行登录验证,验证未通过,账户已锁定", username);
            token.clear();
            return result;
        } catch (ExcessiveAttemptsException e) {
            logger.error("对用户[{}]进行登录验证,验证未通过,错误次数过多", username);
            token.clear();
            return result;
        } catch (AuthenticationException e) {
            logger.error("对用户[{}]进行登录验证,验证未通过,堆栈轨迹如下", username, e);
            token.clear();
            return result;
        }

        // 从session中获取 user 对象
        Session session = SecurityUtils.getSubject().getSession();
        SysUser user = (SysUser) session.getAttribute("USER_SESSION");

        result.put("user",user);
        return result;
    }

}
