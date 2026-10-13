-- ============================================================
-- 阶段1+2：设备侧（手表上报）数据库升级脚本
-- 新增：device（手表设备档案）、device_location（位置流水）、bind_code（设备绑定码）
-- 用法：mysql -uroot -p123456 --default-character-set=utf8mb4 old < device_migration.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `device`
-- 手表设备档案：设备号 + 密钥鉴权，绑定到某位老人
-- ----------------------------
CREATE TABLE IF NOT EXISTS `device` (
  `device_id` int(10) NOT NULL auto_increment,
  `device_no` varchar(32) NOT NULL COMMENT '设备号（手表端登录用，如 W3F8K2M9）',
  `secret` varchar(64) NOT NULL COMMENT '设备密钥（手表端登录用）',
  `alias` varchar(32) default NULL COMMENT '设备别名/型号',
  `model` varchar(32) default NULL COMMENT '型号，如 HarmonyOS-Wear',
  `app_version` varchar(20) default NULL COMMENT '手表端应用版本',
  `battery` int(5) default NULL COMMENT '电量百分比（最近一次上报）',
  `old_id` int(10) default NULL COMMENT '绑定的老人ID，NULL 表示未绑定',
  `family_id` int(10) default NULL COMMENT '绑定操作人（家属ID）',
  `status` tinyint(1) NOT NULL default '1' COMMENT '1-启用 0-停用',
  `bind_time` datetime default NULL COMMENT '绑定时间',
  `last_time` datetime default NULL COMMENT '最后一次心跳/上报时间',
  `last_lng` varchar(20) default NULL COMMENT '最近经度',
  `last_lat` varchar(20) default NULL COMMENT '最近纬度',
  `create_time` datetime default NULL COMMENT '创建时间',
  PRIMARY KEY  (`device_id`),
  UNIQUE KEY `uk_device_no` (`device_no`),
  KEY `idx_device_old` (`old_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `device_location`
-- 位置流水：手表每次上报的位置都记一条，页面取最新一条
-- ----------------------------
CREATE TABLE IF NOT EXISTS `device_location` (
  `id` int(10) NOT NULL auto_increment,
  `device_id` int(10) default NULL,
  `old_id` int(10) default NULL,
  `lng` varchar(20) default NULL COMMENT '经度',
  `lat` varchar(20) default NULL COMMENT '纬度',
  `source` varchar(20) default NULL COMMENT '定位来源：gps/wifi/lbs',
  `be_time` datetime default NULL COMMENT '定位时间（设备侧时间）',
  `create_time` datetime default NULL COMMENT '入库时间',
  PRIMARY KEY  (`id`),
  KEY `idx_loc_old_time` (`old_id`, `id`),
  KEY `idx_loc_device_time` (`device_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `bind_code`
-- 设备绑定码：老人/家属/管理员生成，在手表上输入即可完成绑定
-- ----------------------------
CREATE TABLE IF NOT EXISTS `bind_code` (
  `id` int(10) NOT NULL auto_increment,
  `code` varchar(20) NOT NULL COMMENT '绑定码',
  `old_id` int(10) default NULL COMMENT '要绑定的老人ID',
  `family_id` int(10) default NULL COMMENT '生成该码的家属ID（管理员生成时为空）',
  `create_user` varchar(20) default NULL COMMENT '生成人',
  `create_time` datetime default NULL,
  `expire_time` datetime default NULL COMMENT '过期时间，NULL 表示长期有效',
  `used` tinyint(1) NOT NULL default '0' COMMENT '0-未使用 1-已使用',
  `used_time` datetime default NULL,
  `used_device_id` int(10) default NULL COMMENT '使用该码绑定的设备ID',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `uk_bind_code` (`code`),
  KEY `idx_bind_code_old` (`old_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- 预置演示设备（对应 seed 数据里的 3 块手表 device_id=1/2/3）
-- secret 首次部署后请用“设备管理-重置密钥”功能更换
-- ----------------------------
INSERT INTO `device` (`device_id`,`device_no`,`secret`,`alias`,`model`,`old_id`,`status`,`bind_time`,`create_time`)
SELECT 1,'WATCH0001','demo-secret-1','演示手表1号','HarmonyOS-Wear',1,1,NOW(),NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `device` WHERE `device_id`=1);

INSERT INTO `device` (`device_id`,`device_no`,`secret`,`alias`,`model`,`old_id`,`status`,`bind_time`,`create_time`)
SELECT 2,'WATCH0002','demo-secret-2','演示手表2号','HarmonyOS-Wear',2,1,NOW(),NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `device` WHERE `device_id`=2);

INSERT INTO `device` (`device_id`,`device_no`,`secret`,`alias`,`model`,`old_id`,`status`,`bind_time`,`create_time`)
SELECT 3,'WATCH0003','demo-secret-3','演示手表3号','HarmonyOS-Wear',3,1,NOW(),NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `device` WHERE `device_id`=3);

-- ----------------------------
-- watch_day_info：保证每台设备每天只有一条记录（睡眠步数按天累计）
-- 先清理历史重复行，再加唯一索引；重复执行也不会报错
-- ----------------------------
DELETE w1 FROM watch_day_info w1
  INNER JOIN watch_day_info w2
    ON w1.device_id = w2.device_id AND w1.create_time = w2.create_time AND w1.id < w2.id;

SET @has_uk := (SELECT COUNT(1) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'watch_day_info'
                  AND INDEX_NAME = 'uk_day_device_date');
SET @sql_uk := IF(@has_uk = 0,
  'ALTER TABLE `watch_day_info` ADD UNIQUE KEY `uk_day_device_date` (`device_id`,`create_time`)',
  'SELECT 1');
PREPARE stmt_uk FROM @sql_uk;
EXECUTE stmt_uk;
DEALLOCATE PREPARE stmt_uk;

-- ----------------------------
-- 异常处置（红点）相关字段：给设备与心率血压记录加"已处理"标记
-- 红点规则：异常标记=1 且 未处理(handled=0) -> 显示红点；处置后 handled=1，红点消失
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device' AND COLUMN_NAME = 'person_handled');
SET @s := IF(@c = 0,
  'ALTER TABLE `device` ADD COLUMN `person_handled` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''人员不合格是否已处理''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device' AND COLUMN_NAME = 'rate_handled');
SET @s := IF(@c = 0,
  'ALTER TABLE `device` ADD COLUMN `rate_handled` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''心率不合格是否已处理''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device' AND COLUMN_NAME = 'pre_handled');
SET @s := IF(@c = 0,
  'ALTER TABLE `device` ADD COLUMN `pre_handled` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''血压不合格是否已处理''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ratepre' AND COLUMN_NAME = 'rate_handled');
SET @s := IF(@c = 0,
  'ALTER TABLE `ratepre` ADD COLUMN `rate_handled` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''心率异常是否已处理''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ratepre' AND COLUMN_NAME = 'pre_handled');
SET @s := IF(@c = 0,
  'ALTER TABLE `ratepre` ADD COLUMN `pre_handled` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''血压异常是否已处理''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 历史数据（升级前就存在的异常）视为已处理，避免一打开页面满屏红点
UPDATE `ratepre` SET `rate_handled` = 1 WHERE `create_time` < CURDATE() AND `rate_handled` = 0;
UPDATE `ratepre` SET `pre_handled` = 1 WHERE `create_time` < CURDATE() AND `pre_handled` = 0;
UPDATE `alarm_precess` SET `status` = '0'
 WHERE `status` = '1' AND (`be_time` IS NULL OR `be_time` < CURDATE());

SET FOREIGN_KEY_CHECKS=1;
