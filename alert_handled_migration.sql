-- ============================================================
-- 异常处置升级：给告警/异常记录加"已处理"标记
--   红点规则：异常标记(rate_state/pre_state/status)=1 且 未处理(handled=0) -> 显示红点
--   处置后 handled=1，红点消失，同时把异常标记改回 0（进入已处理历史）
-- 用法：mysql -uroot -p123456 --default-character-set=utf8mb4 old < alert_handled_migration.sql
-- 可重复执行
-- ============================================================

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- ratepre：心率异常是否已处理
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ratepre' AND COLUMN_NAME = 'rate_handled');
SET @s := IF(@c = 0,
  'ALTER TABLE `ratepre` ADD COLUMN `rate_handled` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''心率异常是否已处理：0-未处理 1-已处理''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ----------------------------
-- ratepre：血压异常是否已处理
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ratepre' AND COLUMN_NAME = 'pre_handled');
SET @s := IF(@c = 0,
  'ALTER TABLE `ratepre` ADD COLUMN `pre_handled` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''血压异常是否已处理：0-未处理 1-已处理''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 历史数据（升级前就存在的异常记录）视为已处理，避免一打开页面就一堆红点
UPDATE `ratepre` SET `rate_handled` = 1 WHERE `create_time` < CURDATE() AND `rate_handled` = 0;
UPDATE `ratepre` SET `pre_handled` = 1 WHERE `create_time` < CURDATE() AND `pre_handled` = 0;

-- ----------------------------
-- device：三类"巡检不合格"是否已处理
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

-- ----------------------------
-- alarm_precess：摔倒/告警的处置状态
-- status='1' 表示告警发生（红点），处置后置为 '0'（已处理）
-- 这里只把"历史遗留的样例数据"标为已处理，今天的告警保持红点
-- ----------------------------
UPDATE `alarm_precess` SET `status` = '0'
 WHERE `status` = '1' AND (`be_time` IS NULL OR `be_time` < CURDATE());

SET FOREIGN_KEY_CHECKS=1;
