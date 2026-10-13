-- ============================================================
-- 邀请码注册功能 —— 数据库升级脚本
-- 适用场景：数据库里已经导入过 old.sql，只是需要补上邀请码表
-- 用法：mysql -uroot -p123456 --default-character-set=utf8mb4 old < invite_code_migration.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `invite_code`
-- 邀请码：管理员生成，家属凭邀请码自助注册
-- ----------------------------
CREATE TABLE IF NOT EXISTS `invite_code` (
  `id` int(10) NOT NULL auto_increment,
  `code` varchar(20) NOT NULL COMMENT '邀请码',
  `old_id` int(10) default NULL COMMENT '绑定的老人ID，注册后自动关联该老人',
  `create_user` varchar(20) default '管理员' COMMENT '生成人',
  `create_time` datetime default NULL COMMENT '生成时间',
  `expire_time` datetime default NULL COMMENT '过期时间，NULL 表示长期有效',
  `used` tinyint(1) NOT NULL default '0' COMMENT '是否已使用：0-未使用 1-已使用',
  `used_time` datetime default NULL COMMENT '使用时间',
  `used_family_id` int(10) default NULL COMMENT '使用该邀请码注册的家属ID',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `uk_invite_code_code` (`code`),
  KEY `idx_invite_code_old` (`old_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS=1;
