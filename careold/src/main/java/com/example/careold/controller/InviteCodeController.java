package com.example.careold.controller;

import com.example.careold.common.DateConst;
import com.example.careold.common.ReturnCodeUtil;
import com.example.careold.dao.FamilyDao;
import com.example.careold.dao.InviteCodeDao;
import com.example.careold.dao.OldPersonDao;
import com.example.careold.dao.UserDao;
import com.example.careold.domain.Family;
import com.example.careold.domain.InviteCode;
import com.example.careold.domain.InviteCodeDto;
import com.example.careold.domain.OldPerson;
import com.example.careold.domain.Users;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 邀请码注册
 * 管理员生成邀请码 -> 家属凭邀请码自助注册账号（注册后自动绑定邀请码指定的老人）
 */
@Controller
@RequestMapping("/restful/inviteCode")
public class InviteCodeController {

    private static final Logger logger = LoggerFactory.getLogger(InviteCodeController.class);

    /** 正在使用 / 已失效 的提示码，沿用 SuccessCode/FalseCode 风格 */
    private static final String CODE_INVALID = "0001";
    private static final String CODE_USED = "0002";
    private static final String CODE_EXPIRED = "0003";

    /** 手机号格式：11 位，1 开头 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    /** 单次生成的邀请码数量上限 */
    private static final int MAX_BATCH = 50;

    @Autowired
    InviteCodeDao inviteCodeDao;

    @Autowired
    OldPersonDao oldPersonDao;

    @Autowired
    FamilyDao familyDao;

    @Autowired
    UserDao userDao;

    // ==================== 管理员：邀请码管理 ====================

    /**
     * 邀请码列表
     * 入参：state = all（默认）/ unused / used / expired
     */
    @ResponseBody
    @PostMapping("/list")
    public ListOrderedMap list(@RequestBody(required = false) ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String state = paramValue(param, "state", "all").toLowerCase();

        List<InviteCodeDto> all = inviteCodeDao.findInviteCodes(-1);
        List<ListOrderedMap> rows = new ArrayList<>();
        Date now = new Date();
        for (InviteCodeDto dto : all) {
            String status = statusOf(dto, now);
            if (!matchState(state, status)) {
                continue;
            }
            rows.add(toView(dto, status, now));
        }
        result.put("inviteCodes", rows);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 生成邀请码
     * 入参：oldId（必填）、expireDays（可选，为空表示长期有效）
     */
    @ResponseBody
    @PostMapping("/create")
    public ListOrderedMap create(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();

        String oldIdStr = paramValue(param, "oldId", "");
        if (!ReturnCodeUtil.isNumeric(oldIdStr) || "".equals(oldIdStr)) {
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.falseCode);
            result.put("message", "请选择邀请码绑定的老人");
            return result;
        }
        int oldId = Integer.parseInt(oldIdStr);
        OldPerson oldPerson = oldPersonDao.getOldPersonById(oldId);
        if (oldPerson == null) {
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.falseCode);
            result.put("message", "老人不存在，请重新选择");
            return result;
        }

        // 有效天数：缺省或非法按长期有效处理；上限 3650 天（10 年）
        int expireDays = 0;
        String expireDaysStr = paramValue(param, "expireDays", "");
        if (expireDaysStr.length() > 0) {
            try {
                expireDays = Integer.parseInt(expireDaysStr.trim());
            } catch (NumberFormatException e) {
                expireDays = 0;
            }
            if (expireDays < 0) {
                expireDays = 0;
            }
            if (expireDays > 3650) {
                expireDays = 3650;
            }
        }

        // 生成数量：默认 1，最多 MAX_BATCH 个
        int count = 1;
        String countStr = paramValue(param, "count", "");
        if (countStr.length() > 0) {
            try {
                count = Integer.parseInt(countStr.trim());
            } catch (NumberFormatException e) {
                count = 1;
            }
            if (count < 1) {
                count = 1;
            }
            if (count > MAX_BATCH) {
                count = MAX_BATCH;
            }
        }

        String createUser = paramValue(param, "createUser", "管理员");
        if (createUser.length() == 0) {
            createUser = "管理员";
        }

        String expireTime = null;
        if (expireDays > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, expireDays);
            expireTime = DateConst.dateToString(calendar.getTime(), DateConst.DATE_MODEL_2);
        }

        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            InviteCode inviteCode = new InviteCode();
            inviteCode.setOldId(oldId);
            inviteCode.setCreateUser(createUser);
            inviteCode.setExpireTime(expireTime);
            // 先占位生成，写入前已校验过唯一性
            inviteCode.setCode(inviteCodeDao.generateCode());
            Integer id = inviteCodeDao.save(inviteCode);
            if (id == null) {
                logger.error("生成邀请码失败，oldId={}", oldId);
                result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.falseCode);
                result.put("message", "生成邀请码失败，请稍后重试");
                return result;
            }
            codes.add(inviteCode.getCode());
        }

        result.put("codes", codes);
        result.put("oldId", oldId);
        result.put("oldPersonName", oldPerson.getName());
        result.put("expireTime", expireTime);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 删除（作废）未被使用的邀请码
     * 入参：id
     */
    @ResponseBody
    @PostMapping("/delete")
    public ListOrderedMap delete(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        String idStr = paramValue(param, "id", "");
        if (!ReturnCodeUtil.isNumeric(idStr) || "".equals(idStr)) {
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.falseCode);
            result.put("message", "参数不正确");
            return result;
        }
        int rows = inviteCodeDao.deleteUnused(Integer.parseInt(idStr));
        if (rows == 1) {
            result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
            return result;
        }
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.falseCode);
        result.put("message", "邀请码已被使用或不存在，无法作废");
        return result;
    }

    /**
     * 后台选择老人用的下拉列表
     */
    @ResponseBody
    @PostMapping("/oldPersonList")
    public ListOrderedMap oldPersonList(@RequestBody(required = false) ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        List<OldPerson> oldPeople = oldPersonDao.getOldPerson("");
        List<ListOrderedMap> rows = new ArrayList<>();
        for (OldPerson oldPerson : oldPeople) {
            ListOrderedMap row = new ListOrderedMap();
            row.put("oldId", oldPerson.getOldId());
            row.put("name", oldPerson.getName());
            row.put("phone", oldPerson.getPhone());
            row.put("room", oldPerson.getRoom());
            rows.add(row);
        }
        result.put("oldPeople", rows);
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    // ==================== 注册（公开接口） ====================

    /**
     * 校验邀请码，供注册页实时提示
     * 入参：code
     */
    @ResponseBody
    @PostMapping("/checkCode")
    public ListOrderedMap checkCode(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();
        InviteCodeDto inviteCode = inviteCodeDao.findByCode(normalizeCode(paramValue(param, "code", "")));
        if (inviteCode == null) {
            result.put(ReturnCodeUtil.returnCode, CODE_INVALID);
            result.put("message", "邀请码不存在，请向管理员确认");
            return result;
        }
        String status = statusOf(inviteCode, new Date());
        if ("used".equals(status)) {
            result.put(ReturnCodeUtil.returnCode, CODE_USED);
            result.put("message", "该邀请码已被使用");
            return result;
        }
        if ("expired".equals(status)) {
            result.put(ReturnCodeUtil.returnCode, CODE_EXPIRED);
            result.put("message", "该邀请码已过期，请向管理员重新获取");
            return result;
        }
        result.put("oldPersonName", inviteCode.getOldPersonName());
        result.put("expireTime", inviteCode.getExpireTime() == null ? "" : inviteCode.getExpireTime());
        result.put("remainDays", remainDays(inviteCode.getExpireTime()));
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    /**
     * 家属凭邀请码注册
     * 入参：code、familyName、familyPhone、familyPassword、familyRelation、familySex、familyAddress
     */
    @ResponseBody
    @PostMapping("/join")
    public ListOrderedMap join(@RequestBody ListOrderedMap param) {
        ListOrderedMap result = new ListOrderedMap();

        String code = normalizeCode(paramValue(param, "code", ""));
        String familyName = paramValue(param, "familyName", "").trim();
        String familyPhone = paramValue(param, "familyPhone", "").trim();
        String familyPassword = paramValue(param, "familyPassword", "");
        String familyRelation = paramValue(param, "familyRelation", "").trim();
        String familySex = paramValue(param, "familySex", "").trim();
        String familyAddress = paramValue(param, "familyAddress", "").trim();

        if (code.length() == 0) {
            return fail(result, "请输入邀请码");
        }
        if (familyName.length() == 0) {
            return fail(result, "请输入姓名");
        }
        if (familyName.length() > 10) {
            return fail(result, "姓名长度不能超过10个字符");
        }
        if (!PHONE_PATTERN.matcher(familyPhone).matches()) {
            return fail(result, "请输入正确的11位手机号");
        }
        if (familyPassword.length() < 6 || familyPassword.length() > 20) {
            return fail(result, "密码长度需为6-20个字符");
        }
        if (familyRelation.length() > 10) {
            return fail(result, "与老人关系长度不能超过10个字符");
        }
        if (familyAddress.length() > 20) {
            return fail(result, "地址长度不能超过20个字符");
        }
        if (!"男".equals(familySex) && !"女".equals(familySex)) {
            return fail(result, "请选择性别");
        }

        InviteCodeDto inviteCode = inviteCodeDao.findByCode(code);
        if (inviteCode == null) {
            return fail(result, "邀请码不存在，请向管理员确认");
        }
        String status = statusOf(inviteCode, new Date());
        if ("used".equals(status)) {
            return fail(result, "该邀请码已被使用");
        }
        if ("expired".equals(status)) {
            return fail(result, "该邀请码已过期，请向管理员重新获取");
        }

        if (isPhoneRegistered(familyPhone)) {
            return fail(result, "该手机号已注册，请直接登录或更换手机号");
        }

        OldPerson oldPerson = oldPersonDao.getOldPersonById(inviteCode.getOldId());
        if (oldPerson == null) {
            return fail(result, "邀请码绑定的老人不存在，请联系管理员");
        }

        Family family = new Family();
        family.setFamilyName(familyName);
        family.setFamilyRelation(familyRelation);
        family.setFamilyPhone(familyPhone);
        family.setFamilySex(familySex);
        family.setFamilyAddress(familyAddress);
        family.setFamilyPassword(familyPassword);

        Integer familyId = familyDao.saveAndGetId(family);
        if (familyId == null) {
            logger.error("家属注册失败，手机号={}", familyPhone);
            return fail(result, "注册失败，请稍后重试");
        }

        // 注册成功后自动把家属与邀请码指定的老人绑定
        oldPersonDao.updateOldPerson2(oldPerson.getOldId(), familyId);

        // 邀请码一次性使用，这里做原子更新，避免同一邀请码被重复使用
        int rows = inviteCodeDao.markUsed(inviteCode.getId(), familyId,
                DateConst.dateToString(new Date(), DateConst.DATE_MODEL_2));
        if (rows != 1) {
            logger.error("邀请码占用失败，code={}，familyId={}", code, familyId);
            return fail(result, "该邀请码已被使用，请向管理员重新获取");
        }

        result.put("familyId", familyId);
        result.put("familyName", familyName);
        result.put("oldPersonName", oldPerson.getName());
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.successCode);
        return result;
    }

    // ==================== 内部方法 ====================

    private ListOrderedMap fail(ListOrderedMap result, String message) {
        result.put(ReturnCodeUtil.returnCode, ReturnCodeUtil.falseCode);
        result.put("message", message);
        return result;
    }

    /** 该手机号是否已在管理员(users)或家属(family)中存在 */
    private boolean isPhoneRegistered(String phone) {
        List<Users> users = userDao.getUsersByPhone2(phone);
        if (users != null && !users.isEmpty()) {
            return true;
        }
        return familyDao.countByPhone(phone) > 0;
    }

    /** 邀请码状态：unused / used / expired */
    private String statusOf(InviteCode inviteCode, Date now) {
        if (inviteCode.getUsed() != null && inviteCode.getUsed() == 1) {
            return "used";
        }
        if (isExpired(inviteCode.getExpireTime(), now)) {
            return "expired";
        }
        return "unused";
    }

    private boolean isExpired(String expireTime, Date now) {
        if (expireTime == null || expireTime.trim().length() == 0) {
            return false;
        }
        try {
            Date expire = DateConst.toDate(expireTime.trim(), DateConst.DATE_MODEL_2);
            return expire != null && expire.getTime() <= now.getTime();
        } catch (Exception e) {
            logger.error("邀请码过期时间格式错误：{}", expireTime);
            return false;
        }
    }

    private boolean matchState(String state, String status) {
        if (state == null || state.length() == 0 || "all".equals(state)) {
            return true;
        }
        if ("unused".equals(state) || "used".equals(state) || "expired".equals(state)) {
            return state.equals(status);
        }
        return true;
    }

    private ListOrderedMap toView(InviteCodeDto dto, String status, Date now) {
        ListOrderedMap row = new ListOrderedMap();
        row.put("id", dto.getId());
        row.put("code", dto.getCode());
        row.put("oldId", dto.getOldId());
        row.put("oldPersonName", dto.getOldPersonName() == null ? "（老人已删除）" : dto.getOldPersonName());
        row.put("createUser", dto.getCreateUser());
        row.put("createTime", dto.getCreateTime());
        row.put("expireTime", dto.getExpireTime() == null ? "" : dto.getExpireTime());
        row.put("remainDays", remainDays(dto.getExpireTime()));
        row.put("usedTime", dto.getUsedTime() == null ? "" : dto.getUsedTime());
        row.put("status", status);
        if ("used".equals(status)) {
            row.put("statusText", "已使用");
        } else if ("expired".equals(status)) {
            row.put("statusText", "已过期");
        } else {
            row.put("statusText", "可使用");
        }
        return row;
    }

    /** 剩余有效天数，长期有效返回 -1 */
    private int remainDays(String expireTime) {
        if (expireTime == null || expireTime.trim().length() == 0) {
            return -1;
        }
        try {
            Date expire = DateConst.toDate(expireTime.trim(), DateConst.DATE_MODEL_2);
            if (expire == null) {
                return -1;
            }
            long diff = expire.getTime() - System.currentTimeMillis();
            if (diff <= 0) {
                return 0;
            }
            return (int) ((diff + 24L * 60 * 60 * 1000 - 1) / (24L * 60 * 60 * 1000));
        } catch (Exception e) {
            return -1;
        }
    }

    /** 邀请码统一大写、去掉空格和连字符，避免用户输入格式差异 */
    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().replace(" ", "").replace("-", "").toUpperCase();
    }

    /** 取值，键不存在或值为空时返回默认值（老代码直接 toString 会空指针） */
    private String paramValue(ListOrderedMap param, String key, String defaultValue) {
        if (param == null) {
            return defaultValue;
        }
        Object value = param.get(key);
        if (value == null) {
            return defaultValue;
        }
        String str = value.toString().trim();
        return str.length() == 0 ? defaultValue : str;
    }
}
