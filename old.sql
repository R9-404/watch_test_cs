/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50022
Source Host           : localhost:3306
Source Database       : old

Target Server Type    : MYSQL
Target Server Version : 50022
File Encoding         : 65001

Date: 2021-02-01 15:48:48
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `alarm_precess`
-- ----------------------------
DROP TABLE IF EXISTS `alarm_precess`;
CREATE TABLE `alarm_precess` (
  `id` int(20) NOT NULL auto_increment,
  `status` varchar(20) default NULL,
  `old_id` int(10) default NULL,
  `device_id` int(10) default NULL,
  `be_time` datetime default NULL,
  `lng` varchar(20) default NULL COMMENT '经度',
  `lat` varchar(20) default NULL COMMENT '维度',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of alarm_precess
-- ----------------------------
INSERT INTO `alarm_precess` VALUES ('1', '1', '1', '1', '2019-03-24 17:22:14', '121.14598', '31.160181');
INSERT INTO `alarm_precess` VALUES ('2', '0', '2', '2', '2019-04-01 20:32:59', '121.14594', '31.160182');
INSERT INTO `alarm_precess` VALUES ('3', '0', '3', '3', '2019-04-01 20:34:11', '121.14592', '31.160184');

-- ----------------------------
-- Table structure for `customer`
-- ----------------------------
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer` (
  `customer_id` int(10) NOT NULL auto_increment,
  `customer_name` varchar(20) default NULL,
  `customer_age` int(5) default NULL,
  `customer_sex` char(1) default NULL,
  `customer_phone` varchar(12) default NULL,
  `customer_address` varchar(20) default NULL,
  PRIMARY KEY  (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of customer
-- ----------------------------
INSERT INTO `customer` VALUES ('1', '小黄', '21', '男', '15179158706', '南昌');
INSERT INTO `customer` VALUES ('2', '任飞', '20', '男', '15236895628', '南昌');
INSERT INTO `customer` VALUES ('3', '赵燕', '21', '女', '15636259856', '南昌');

-- ----------------------------
-- Table structure for `family`
-- ----------------------------
DROP TABLE IF EXISTS `family`;
CREATE TABLE `family` (
  `family_tou_pic` varchar(50) default 'kai.jpg',
  `family_id` int(10) NOT NULL auto_increment,
  `family_name` varchar(10) default NULL,
  `family_relation` varchar(10) default NULL,
  `family_phone` varchar(12) default NULL,
  `family_sex` char(1) default NULL,
  `family_address` varchar(20) default NULL,
  `family_password` varchar(20) default '123456',
  PRIMARY KEY  (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of family
-- ----------------------------
INSERT INTO `family` VALUES ('92q58PICeSI_1024.jpg', '1', '小刚23', '儿子', '15179158705', '男', '南昌', '123456');
INSERT INTO `family` VALUES ('kai.jpg', '2', '黄封', '儿子', '15179158706', '男', '南昌', '123456');
INSERT INTO `family` VALUES ('kai.jpg', '3', '王高', '儿子', '15179158702', '男', '南昌', '123456');
INSERT INTO `family` VALUES ('kai.jpg', '6', 'asdf', 'asdf', '15263259856', '男', 'asdf', '123456');
INSERT INTO `family` VALUES ('kai.jpg', '7', 'asdfdf', 'asdf2', '15263259842', '男', 'asdf', '123456');

-- ----------------------------
-- Table structure for `invite_code`
-- 邀请码：管理员生成，家属凭邀请码自助注册
-- ----------------------------
DROP TABLE IF EXISTS `invite_code`;
CREATE TABLE `invite_code` (
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

-- ----------------------------
-- Records of invite_code
-- ----------------------------
INSERT INTO `invite_code` VALUES ('1', 'DEMO2024', '1', '管理员', '2019-04-01 09:00:00', NULL, '0', NULL, NULL);

-- ----------------------------
-- Table structure for `nice_count`
-- ----------------------------
DROP TABLE IF EXISTS `nice_count`;
CREATE TABLE `nice_count` (
  `phone` varchar(30) default NULL,
  `said_id` int(20) default NULL,
  `add_id` int(20) NOT NULL auto_increment,
  PRIMARY KEY  (`add_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of nice_count
-- ----------------------------
INSERT INTO `nice_count` VALUES ('12343342334', '1', '1');
INSERT INTO `nice_count` VALUES ('15179158704', '1', '2');
INSERT INTO `nice_count` VALUES ('15179158705', '2', '3');
INSERT INTO `nice_count` VALUES ('15179158705', '1', '8');
INSERT INTO `nice_count` VALUES ('15179158705', '3', '9');
INSERT INTO `nice_count` VALUES ('15179158705', '6', '13');
INSERT INTO `nice_count` VALUES ('15179158706', '6', '14');

-- ----------------------------
-- Table structure for `oldperson`
-- ----------------------------
DROP TABLE IF EXISTS `oldperson`;
CREATE TABLE `oldperson` (
  `old_id` int(20) NOT NULL auto_increment,
  `name` varchar(10) default NULL,
  `age` int(10) default NULL,
  `sex` char(1) default NULL,
  `phone` varchar(12) default NULL,
  `room` varchar(5) default NULL,
  `illness` varchar(20) default NULL,
  `warn` varchar(5) default NULL,
  `food_like` varchar(20) default NULL,
  `food_hate` varchar(20) default NULL,
  `drag` varchar(20) default NULL,
  `family_id` int(20) default NULL,
  `customer_id` int(20) default NULL,
  `be_time` date default NULL,
  `bothdate` date default NULL,
  `device_id` int(30) default NULL,
  PRIMARY KEY  (`old_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of oldperson
-- ----------------------------
INSERT INTO `oldperson` VALUES ('1', '姜涛', '80', '男', '15267899890', '111', '心脏病', '斑斓更', '鱼', '猪肉', '风油精', '1', '1', '2019-05-03', '1939-03-09', '1');
INSERT INTO `oldperson` VALUES ('2', '王明', '80', '男', '15267899890', '111', '心脏病', '斑斓更', '鱼', '猪肉', '风油精', '2', '2', '2019-04-03', '1939-03-09', '2');
INSERT INTO `oldperson` VALUES ('3', '田风', '79', '男', '15478956852', '111', '心脏病', '三鑫口服液', '鱼', '猪肉', '定心丸', '3', '2', '2019-04-09', '1940-06-19', '3');

-- ----------------------------
-- Table structure for `pic`
-- ----------------------------
DROP TABLE IF EXISTS `pic`;
CREATE TABLE `pic` (
  `pic_id` int(20) NOT NULL,
  `pic_name` varchar(50) default NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of pic
-- ----------------------------
INSERT INTO `pic` VALUES ('2', 'kai.jpg');
INSERT INTO `pic` VALUES ('2', '7487939_090818211000_2.jpg');
INSERT INTO `pic` VALUES ('5', '1.jpg');
INSERT INTO `pic` VALUES ('5', '2.jpg');
INSERT INTO `pic` VALUES ('5', '5.jpg');
INSERT INTO `pic` VALUES ('1', 'kai.jpg');
INSERT INTO `pic` VALUES ('6', 'gou2.jpg');

-- ----------------------------
-- Table structure for `pic_show`
-- ----------------------------
DROP TABLE IF EXISTS `pic_show`;
CREATE TABLE `pic_show` (
  `id` int(10) NOT NULL auto_increment,
  `pic_name` varchar(50) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of pic_show
-- ----------------------------
INSERT INTO `pic_show` VALUES ('33', 'tuzi.jpg');

-- ----------------------------
-- Table structure for `ratepre`
-- ----------------------------
DROP TABLE IF EXISTS `ratepre`;
CREATE TABLE `ratepre` (
  `id` int(10) NOT NULL auto_increment,
  `device_id` int(10) default NULL,
  `old_id` int(10) default NULL,
  `rate_state` varchar(5) default NULL,
  `pre_state` varchar(5) default NULL,
  `create_time` datetime default NULL,
  `hr` int(10) default NULL,
  `bp` int(10) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of ratepre
-- ----------------------------
INSERT INTO `ratepre` VALUES ('1', '1', '1', '0', '1', '2019-03-24 17:26:14', '92', '85');
INSERT INTO `ratepre` VALUES ('2', '1', '1', '0', '1', '2019-03-24 17:27:54', '84', '84');
INSERT INTO `ratepre` VALUES ('3', '1', '1', '0', '1', '2019-03-24 17:28:11', '81', '83');
INSERT INTO `ratepre` VALUES ('4', '1', '1', '0', '1', '2019-03-24 17:29:31', '82', '82');
INSERT INTO `ratepre` VALUES ('5', '2', '2', '1', '0', '2019-03-24 17:30:59', '83', '86');
INSERT INTO `ratepre` VALUES ('6', '3', '3', '1', '0', '2019-03-24 17:29:17', '82', '86');
INSERT INTO `ratepre` VALUES ('7', '1', '1', '0', '1', '2019-03-24 17:25:14', '90', '90');
INSERT INTO `ratepre` VALUES ('8', '1', '1', '0', '1', '2019-03-24 17:24:14', '89', '79');
INSERT INTO `ratepre` VALUES ('9', '1', '1', '0', '1', '2019-03-24 17:23:14', '80', '84');
INSERT INTO `ratepre` VALUES ('10', '2', '2', '0', '1', '2019-03-24 17:31:31', '81', '84');
INSERT INTO `ratepre` VALUES ('11', '2', '2', '0', '1', '2019-03-24 17:32:31', '82', '83');
INSERT INTO `ratepre` VALUES ('12', '2', '2', '0', '1', '2019-03-24 17:33:31', '83', '90');
INSERT INTO `ratepre` VALUES ('13', '2', '2', '0', '1', '2019-03-24 17:34:31', '80', '95');
INSERT INTO `ratepre` VALUES ('14', '2', '2', '0', '1', '2019-03-24 17:35:31', '85', '92');
INSERT INTO `ratepre` VALUES ('15', '2', '2', '0', '1', '2019-03-24 17:36:31', '134', '95');
INSERT INTO `ratepre` VALUES ('16', '2', '2', '0', '1', '2019-03-24 17:37:31', '132', '85');
INSERT INTO `ratepre` VALUES ('17', '2', '2', '0', '1', '2019-03-24 17:38:31', '126', '89');
INSERT INTO `ratepre` VALUES ('18', '2', '2', '0', '1', '2019-03-24 17:39:31', '85', '85');
INSERT INTO `ratepre` VALUES ('19', '2', '2', '0', '1', '2019-03-24 17:40:31', '82', '89');
INSERT INTO `ratepre` VALUES ('20', '2', '2', '0', '1', '2019-03-24 17:41:31', '83', '80');
INSERT INTO `ratepre` VALUES ('21', '2', '2', '0', '1', '2019-03-24 17:42:31', '56', '80');
INSERT INTO `ratepre` VALUES ('22', '2', '2', '0', '1', '2019-03-24 17:43:31', '80', '80');
INSERT INTO `ratepre` VALUES ('23', '2', '2', '0', '1', '2019-03-24 17:44:31', '84', '89');
INSERT INTO `ratepre` VALUES ('24', '2', '2', '0', '1', '2019-03-24 17:45:31', '97', '92');
INSERT INTO `ratepre` VALUES ('25', '2', '2', '0', '1', '2019-03-24 17:46:31', '87', '89');
INSERT INTO `ratepre` VALUES ('26', '2', '2', '0', '1', '2019-03-24 17:47:31', '98', '82');
INSERT INTO `ratepre` VALUES ('27', '2', '2', '0', '1', '2019-03-24 17:48:31', '90', '85');
INSERT INTO `ratepre` VALUES ('28', '2', '2', '0', '1', '2019-03-24 17:50:31', '80', '100');
INSERT INTO `ratepre` VALUES ('29', '2', '2', '0', '1', '2019-03-24 17:51:31', '85', '86');
INSERT INTO `ratepre` VALUES ('30', '2', '2', '0', '1', '2019-03-24 17:52:31', '87', '80');
INSERT INTO `ratepre` VALUES ('31', '2', '2', '0', '1', '2019-03-24 17:53:31', '96', '87');
INSERT INTO `ratepre` VALUES ('32', '2', '2', '0', '1', '2019-03-24 17:54:31', '85', '96');
INSERT INTO `ratepre` VALUES ('33', '2', '2', '0', '1', '2019-03-24 17:55:31', '83', '90');
INSERT INTO `ratepre` VALUES ('34', '2', '2', '0', '1', '2019-03-24 17:56:31', '86', '95');
INSERT INTO `ratepre` VALUES ('35', '2', '2', '0', '1', '2019-03-24 17:57:31', '87', '103');
INSERT INTO `ratepre` VALUES ('36', '2', '2', '0', '1', '2019-03-24 17:58:31', '99', '103');
INSERT INTO `ratepre` VALUES ('37', '2', '2', '0', '1', '2019-03-24 17:59:31', '90', '80');
INSERT INTO `ratepre` VALUES ('38', '3', '3', '0', '1', '2019-03-24 17:36:31', '90', '80');
INSERT INTO `ratepre` VALUES ('39', '3', '3', '0', '1', '2019-03-24 17:37:31', '96', '85');
INSERT INTO `ratepre` VALUES ('40', '3', '3', '0', '1', '2019-03-24 17:38:31', '98', '89');
INSERT INTO `ratepre` VALUES ('41', '3', '3', '0', '1', '2019-03-24 17:39:31', '92', '85');
INSERT INTO `ratepre` VALUES ('42', '3', '3', '0', '1', '2019-03-24 17:40:31', '99', '89');
INSERT INTO `ratepre` VALUES ('43', '3', '3', '0', '1', '2019-03-24 17:41:31', '92', '63');
INSERT INTO `ratepre` VALUES ('44', '3', '3', '0', '1', '2019-03-24 17:42:31', '86', '96');
INSERT INTO `ratepre` VALUES ('45', '3', '3', '0', '1', '2019-03-24 17:43:31', '96', '100');
INSERT INTO `ratepre` VALUES ('46', '3', '3', '0', '1', '2019-03-24 17:44:31', '83', '85');
INSERT INTO `ratepre` VALUES ('47', '3', '3', '0', '1', '2019-03-24 17:45:31', '84', '82');
INSERT INTO `ratepre` VALUES ('48', '3', '3', '0', '1', '2019-03-24 17:46:31', '88', '80');
INSERT INTO `ratepre` VALUES ('49', '3', '3', '0', '1', '2019-03-24 17:47:31', '98', '90');
INSERT INTO `ratepre` VALUES ('50', '3', '3', '0', '1', '2019-03-24 17:48:31', '99', '91');
INSERT INTO `ratepre` VALUES ('51', '3', '3', '0', '1', '2019-03-24 17:49:31', '95', '90');
INSERT INTO `ratepre` VALUES ('52', '3', '3', '0', '1', '2019-03-24 17:50:31', '102', '85');
INSERT INTO `ratepre` VALUES ('53', '3', '3', '0', '1', '2019-03-24 17:51:31', '100', '83');
INSERT INTO `ratepre` VALUES ('54', '3', '3', '0', '1', '2019-03-24 17:52:31', '104', '88');
INSERT INTO `ratepre` VALUES ('55', '3', '3', '0', '1', '2019-03-24 17:53:31', '100', '82');
INSERT INTO `ratepre` VALUES ('56', '3', '3', '0', '1', '2019-03-24 17:54:31', '105', '87');
INSERT INTO `ratepre` VALUES ('57', '3', '3', '0', '1', '2019-03-24 17:55:31', '130', '88');
INSERT INTO `ratepre` VALUES ('58', '3', '3', '0', '1', '2019-03-24 17:56:31', '126', '88');
INSERT INTO `ratepre` VALUES ('59', '3', '3', '0', '1', '2019-03-24 17:57:31', '123', '99');
INSERT INTO `ratepre` VALUES ('60', '1', '1', '0', '1', '2019-03-24 17:30:31', '100', '99');
INSERT INTO `ratepre` VALUES ('61', '1', '1', '0', '1', '2019-03-24 17:31:31', '98', '98');
INSERT INTO `ratepre` VALUES ('62', '1', '1', '0', '1', '2019-03-24 17:32:31', '87', '95');
INSERT INTO `ratepre` VALUES ('63', '1', '1', '0', '1', '2019-03-24 17:33:31', '85', '94');
INSERT INTO `ratepre` VALUES ('64', '1', '1', '0', '1', '2019-03-24 17:34:31', '86', '95');
INSERT INTO `ratepre` VALUES ('65', '1', '1', '0', '1', '2019-03-24 17:35:31', '87', '94');
INSERT INTO `ratepre` VALUES ('66', '1', '1', '0', '1', '2019-03-24 17:36:31', '87', '50');
INSERT INTO `ratepre` VALUES ('67', '1', '1', '0', '1', '2019-03-24 17:37:31', '89', '52');
INSERT INTO `ratepre` VALUES ('68', '1', '1', '0', '1', '2019-03-24 17:38:31', '87', '55');
INSERT INTO `ratepre` VALUES ('69', '1', '1', '0', '1', '2019-03-24 17:39:31', '89', '60');
INSERT INTO `ratepre` VALUES ('70', '1', '1', '0', '1', '2019-03-24 17:40:31', '85', '67');
INSERT INTO `ratepre` VALUES ('71', '1', '1', '0', '1', '2019-03-24 17:40:31', '103', '70');
INSERT INTO `ratepre` VALUES ('72', '1', '1', '0', '1', '2019-03-24 17:41:31', '106', '71');
INSERT INTO `ratepre` VALUES ('73', '1', '1', '0', '1', '2019-03-24 17:42:31', '103', '74');
INSERT INTO `ratepre` VALUES ('74', '1', '1', '0', '1', '2019-03-24 17:43:31', '107', '79');
INSERT INTO `ratepre` VALUES ('75', '1', '1', '0', '1', '2019-03-24 17:44:31', '102', '81');
INSERT INTO `ratepre` VALUES ('76', '1', '1', '0', '1', '2019-03-24 17:45:31', '109', '86');
INSERT INTO `ratepre` VALUES ('77', '1', '1', '0', '1', '2019-03-24 17:46:31', '101', '88');
INSERT INTO `ratepre` VALUES ('78', '1', '1', '0', '1', '2019-03-24 17:47:31', '106', '82');
INSERT INTO `ratepre` VALUES ('79', '1', '1', '0', '1', '2019-03-24 17:48:31', '101', '86');
INSERT INTO `ratepre` VALUES ('80', '1', '1', '0', '1', '2019-03-24 17:49:31', '102', '80');
INSERT INTO `ratepre` VALUES ('81', '1', '1', '0', '1', '2019-03-24 17:50:31', '98', '76');

-- ----------------------------
-- Table structure for `said`
-- ----------------------------
DROP TABLE IF EXISTS `said`;
CREATE TABLE `said` (
  `said_id` int(10) NOT NULL auto_increment,
  `said` varchar(30) default NULL,
  `text_id` int(10) default NULL,
  `back` varchar(20) default NULL,
  `said_time` datetime default NULL,
  `back_time` datetime default NULL,
  `person_name` varchar(10) default NULL,
  `said_phone` varchar(30) default NULL,
  `tou_pic` varchar(50) default NULL,
  `nice_count` int(20) default '0',
  PRIMARY KEY  (`said_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of said
-- ----------------------------
INSERT INTO `said` VALUES ('1', '你好', '1', '你也好', '2019-03-24 17:31:19', '2019-03-24 17:31:25', '小刚', '15179158704', 'kai.jpg', '6');
INSERT INTO `said` VALUES ('4', '变身', '1', '钢铁侠', '2019-04-19 14:10:50', '2019-04-19 15:22:41', '小刚2', '15179158705', '2.jpg', '0');
INSERT INTO `said` VALUES ('5', '写的好', '1', null, '2019-04-19 14:16:12', null, '小刚2', '15179158705', 'kai.jpg', '0');
INSERT INTO `said` VALUES ('6', '你好', '31', null, '2019-04-26 14:54:42', null, '小刚23', '15179158705', '92q58PICeSI_1024.jpg', '2');

-- ----------------------------
-- Table structure for `text`
-- ----------------------------
DROP TABLE IF EXISTS `text`;
CREATE TABLE `text` (
  `text_id` int(10) NOT NULL auto_increment,
  `title` varchar(20) default NULL,
  `text_main` varchar(50) default NULL,
  `time` datetime default NULL,
  `pic_id` int(10) default NULL,
  PRIMARY KEY  (`text_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of text
-- ----------------------------
INSERT INTO `text` VALUES ('1', '老人运动2', '老人每天通过跑步来锻炼身体2', '2019-04-19 15:52:20', '1');
INSERT INTO `text` VALUES ('2', '老人饮食', '老人幸福的吃着自己包的饺子', '2019-03-24 17:34:51', '2');
INSERT INTO `text` VALUES ('5', '老人节目', '老人组队表演节目', '2019-04-18 10:31:52', '5');
INSERT INTO `text` VALUES ('31', '我去二', '去玩儿', '2019-04-26 14:53:37', '6');

-- ----------------------------
-- Table structure for `users`
-- ----------------------------DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `user_id` int(10) NOT NULL auto_increment,
  `user_name` varchar(10) default NULL,
  `password` varchar(50) default NULL,
  `role` varchar(10) default NULL,
  `user_phone` varchar(20) default NULL,
  PRIMARY KEY  (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES ('1', '小刚', '123456', '1', '15179158704');

-- ----------------------------
-- Table structure for `watch_day_info`
-- ----------------------------
DROP TABLE IF EXISTS `watch_day_info`;
CREATE TABLE `watch_day_info` (
  `id` int(10) NOT NULL auto_increment,
  `device_id` int(10) default NULL,
  `sleep` int(10) default NULL,
  `step` int(10) default NULL,
  `sleep_sate` char(1) default NULL,
  `create_time` date default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of watch_day_info
-- ----------------------------
INSERT INTO `watch_day_info` VALUES ('40', '1', '8', '500', '0', '2019-04-01');
INSERT INTO `watch_day_info` VALUES ('41', '1', '9', '500', '0', '2019-04-02');
INSERT INTO `watch_day_info` VALUES ('42', '1', '8', '500', '0', '2019-04-03');
INSERT INTO `watch_day_info` VALUES ('43', '1', '10', '500', '0', '2019-04-04');
INSERT INTO `watch_day_info` VALUES ('44', '1', '8', '500', '0', '2019-04-05');
INSERT INTO `watch_day_info` VALUES ('45', '2', '8', '500', '0', '2019-04-01');
INSERT INTO `watch_day_info` VALUES ('46', '2', '9', '500', '0', '2019-04-02');
INSERT INTO `watch_day_info` VALUES ('47', '2', '10', '500', '0', '2019-04-03');
INSERT INTO `watch_day_info` VALUES ('48', '2', '8', '500', '0', '2019-04-04');
INSERT INTO `watch_day_info` VALUES ('49', '2', '10', '500', '0', '2019-04-05');
INSERT INTO `watch_day_info` VALUES ('50', '3', '8', '500', '0', '2019-04-01');
INSERT INTO `watch_day_info` VALUES ('51', '3', '9', '500', '0', '2019-04-02');
INSERT INTO `watch_day_info` VALUES ('52', '3', '8', '500', '0', '2019-04-03');
INSERT INTO `watch_day_info` VALUES ('53', '3', '4', '500', '1', '2019-04-04');
INSERT INTO `watch_day_info` VALUES ('54', '3', '7', '500', '0', '2019-04-05');

-- ----------------------------
-- Table structure for `watch_info`
-- ----------------------------
DROP TABLE IF EXISTS `watch_info`;
CREATE TABLE `watch_info` (
  `device_id` int(10) NOT NULL auto_increment,
  `op_time` date default NULL,
  `bpf` int(10) default NULL,
  `bpl` int(10) default NULL,
  `hrf` int(10) default NULL,
  `hrl` int(10) default NULL,
  PRIMARY KEY  (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of watch_info
-- ----------------------------
INSERT INTO `watch_info` VALUES ('1', '2019-03-24', '60', '120', '65', '125');
INSERT INTO `watch_info` VALUES ('2', '2019-03-25', '60', '120', '65', '125');
INSERT INTO `watch_info` VALUES ('3', '2019-03-25', '60', '120', '65', '125');
