/*
 @author Qicz

 Date: 13/07/2021 10:18:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ik_sequence
-- ----------------------------
DROP TABLE IF EXISTS `ik_sequence`;
CREATE TABLE `ik_sequence` (
  `current_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `domain` varchar(100) NOT NULL COMMENT '所属领域',
  PRIMARY KEY (`current_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for ik_words
-- ----------------------------
DROP TABLE IF EXISTS `ik_words`;
CREATE TABLE `ik_words` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `word` varchar(200) NOT NULL,
  `word_type` tinyint(4) unsigned NOT NULL COMMENT 'word类型，1主词库，2stop词库',
  `domain` varchar(100) NOT NULL COMMENT '所属领域',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `domain_word` (`word`,`domain`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
