/*
 @author Qicz
 Date: 12/07/2021 13:16:34
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ik_sequence
-- ----------------------------
DROP TABLE IF EXISTS `ik_sequence`;
CREATE TABLE `ik_sequence` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `dictionary` varchar(100) NOT NULL,
  `current_id` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for ik_stop_words
-- ----------------------------
DROP TABLE IF EXISTS `ik_stop_words`;
CREATE TABLE `ik_stop_words` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `word` varchar(200) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for ik_main_words
-- ----------------------------
DROP TABLE IF EXISTS `ik_main_words`;
CREATE TABLE `ik_main_words` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `word` varchar(200) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
