-- ============================================================
-- 红点修复：把"是否已处理"改成"处理时间"（handled_at）
--
-- 原来只用一个 0/1 标记，手表每几秒上报一次异常都会生成**新的**未处理记录，
-- 于是"标记已处理"后几秒钟红点又冒出来。改成记录处理时间后：
--   只有"处理之后新产生的异常"才会再次亮红点，
--   处理之前的历史异常不再重复报红点（历史数据仍然完整保留）。
-- 用法：mysql -uroot -p123456 --default-character-set=utf8mb4 old < alert_handled_at_migration.sql
-- 可重复执行
-- ============================================================

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- ratepre：心率/血压异常的处理时间
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ratepre' AND COLUMN_NAME = 'rate_handled_at');
SET @s := IF(@c = 0,
  'ALTER TABLE `ratepre` ADD COLUMN `rate_handled_at` datetime DEFAULT NULL COMMENT ''心率异常处理时间，此时间之前的异常不再亮红点''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ratepre' AND COLUMN_NAME = 'pre_handled_at');
SET @s := IF(@c = 0,
  'ALTER TABLE `ratepre` ADD COLUMN `pre_handled_at` datetime DEFAULT NULL COMMENT ''血压异常处理时间，此时间之前的异常不再亮红点''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 兼容旧数据：以前标记过"已处理"的记录，把处理时间补成记录时间，等价于"已处理"
UPDATE `ratepre` SET `rate_handled_at` = `create_time`
 WHERE `rate_handled` = 1 AND `rate_handled_at` IS NULL;
UPDATE `ratepre` SET `pre_handled_at` = `create_time`
 WHERE `pre_handled` = 1 AND `pre_handled_at` IS NULL;

-- ----------------------------
-- device：人员不合格的处理时间
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device' AND COLUMN_NAME = 'person_handled_at');
SET @s := IF(@c = 0,
  'ALTER TABLE `device` ADD COLUMN `person_handled_at` datetime DEFAULT NULL COMMENT ''人员不合格处理时间，设备恢复在线后再次掉线会重新亮红点''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

UPDATE `device` SET `person_handled_at` = NOW()
 WHERE `person_handled` = 1 AND `person_handled_at` IS NULL;

SET FOREIGN_KEY_CHECKS=1;

-- ============================================================
-- 补充：人员不合格改用"掉线次数"精确判定（不再靠时间戳比较）
--   boot_seq / handled_seq 用来判断"设备是否重新上线过"：
--     掉线时 boot_seq+1；确认已处理时记 handled_seq=boot_seq；
--     只有 boot_seq > handled_seq（即重新上线后又掉线）才再次报警。
-- ============================================================
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device' AND COLUMN_NAME = 'boot_seq');
SET @s := IF(@c = 0,
  'ALTER TABLE `device` ADD COLUMN `boot_seq` int(10) NOT NULL DEFAULT 0 COMMENT ''离线次数，用于判断设备是否重新上线过''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device' AND COLUMN_NAME = 'handled_seq');
SET @s := IF(@c = 0,
  'ALTER TABLE `device` ADD COLUMN `handled_seq` int(10) NOT NULL DEFAULT 0 COMMENT ''已确认处理的离线次数''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ----------------------------
-- 用"最近一次确认在线的时间"判断设备是否重新上线过（不再依赖额外的状态表）
--   last_online_at = 心跳/上报时写入 NOW()
--   掉线时：如果 last_online_at > last_time（说明是恢复在线之后又掉线），掉线次数+1
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'device' AND COLUMN_NAME = 'last_online_at');
SET @s := IF(@c = 0,
  'ALTER TABLE `device` ADD COLUMN `last_online_at` datetime DEFAULT NULL COMMENT ''最近一次确认在线的时间''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

UPDATE `device` SET `last_online_at` = `last_time` WHERE `last_online_at` IS NULL AND `last_time` IS NOT NULL;

