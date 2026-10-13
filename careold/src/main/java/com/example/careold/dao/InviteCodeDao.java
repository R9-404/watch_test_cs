package com.example.careold.dao;

import com.example.careold.domain.InviteCode;
import com.example.careold.domain.InviteCodeDto;

import java.util.List;

public interface InviteCodeDao {

    /**
     * 查询邀请码列表
     *
     * @param used 是否已使用：0-未使用 1-已使用；-1 表示不过滤
     */
    List<InviteCodeDto> findInviteCodes(int used);

    /** 按邀请码查询（不存在返回 null） */
    InviteCodeDto findByCode(String code);

    /** 生成一个数据库中不存在的邀请码 */
    String generateCode();

    /** 插入邀请码，返回自增主键，失败返回 null */
    Integer save(InviteCode inviteCode);

    /** 标记邀请码已被使用 */
    int markUsed(Integer id, Integer familyId, String usedTime);

    /** 删除未被使用的邀请码 */
    int deleteUnused(Integer id);

    /** 删除邀请码绑定的老人的记录（老人被删除时清理） */
    int deleteByOldId(Integer oldId);
}
