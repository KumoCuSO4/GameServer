SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS gameserver;
CREATE DATABASE IF NOT EXISTS gameserver DEFAULT CHARACTER SET utf8;
USE gameserver;

-- ----------------------------
-- Table structure for player
-- ----------------------------
DROP TABLE IF EXISTS `player`;
CREATE TABLE `player`  (
                         `uid` BIGINT(11) NOT NULL,
                         `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                         `email` varchar(255) NOT NULL,
                         PRIMARY KEY (`uid`) USING BTREE,
                         UNIQUE KEY `idx_email_unique` (`email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;
INSERT INTO `player` values (1, "admin1", "12345678@test.com");
INSERT INTO `player` values (2, "admin2", "12345677@test.com");

-- ----------------------------
-- Table structure for player_item
-- ----------------------------
DROP TABLE IF EXISTS `player_item`;
CREATE TABLE `player_item`  (
                           `uid` BIGINT(11) NOT NULL,
                           `item_id` int(11) NOT NULL,
                           `num` int(11) NOT NULL,
                           INDEX `uid_item`(`uid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;
INSERT INTO `player_item` values (1, 1001, 111);
INSERT INTO `player_item` values (1, 1002, 222);
INSERT INTO `player_item` values (2, 1001, 102);