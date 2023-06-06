/*
 Navicat Premium Data Transfer

 Source Server         : Aka_database
 Source Server Type    : MySQL
 Source Server Version : 50733
 Source Host           : localhost:3306
 Source Schema         : dormitory

 Target Server Type    : MySQL
 Target Server Version : 50733
 File Encoding         : 65001

 Date: 05/06/2023 22:51:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for adjust_room
-- ----------------------------
DROP TABLE IF EXISTS `adjust_room`;
CREATE TABLE `adjust_room`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '账号',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '姓名',
  `currentroom_id` int(11) NOT NULL COMMENT '当前房间',
  `currentbed_id` int(11) NOT NULL COMMENT '当前床位号',
  `towardsroom_id` int(11) NOT NULL COMMENT '目标房间',
  `towardsbed_id` int(11) NOT NULL COMMENT '目标床位号',
  `state` enum('未处理','通过','驳回') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '未处理' COMMENT '申请状态',
  `apply_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '申请时间',
  `finish_time` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of adjust_room
-- ----------------------------
INSERT INTO `adjust_room` VALUES (2, 'stu1', '张三', 1101, 1, 1102, 4, '通过', '2023-02-17 14:35:02', '2023-03-19 23:07:21');
INSERT INTO `adjust_room` VALUES (9, 'stu1', '张三', 1201, 1, 1201, 3, '未处理', '2023-06-03 21:55:50', NULL);

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '姓名',
  `gender` enum('男','女') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '男' COMMENT '性别',
  `age` int(11) NOT NULL COMMENT '年龄',
  `phone_num` varchar(11) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES ('admin', '123456', '大强', '男', 18, '14785412478', NULL, '4eb7dd1d20ad4b68b3b6a39651281537.png');
INSERT INTO `admin` VALUES ('Atest', '123456', '测试管理员', '男', 22, '14785412478', NULL, NULL);

-- ----------------------------
-- Table structure for dorm_build
-- ----------------------------
DROP TABLE IF EXISTS `dorm_build`;
CREATE TABLE `dorm_build`  (
  `dormbuild_id` int(11) NOT NULL COMMENT '宿舍楼号码',
  `dormbuild_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '宿舍楼名称',
  `dormbuild_detail` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '宿舍楼备注',
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dorm_build
-- ----------------------------
INSERT INTO `dorm_build` VALUES (1, '一号楼', '男宿舍', 1);
INSERT INTO `dorm_build` VALUES (2, '二号楼', '女宿舍', 2);
INSERT INTO `dorm_build` VALUES (3, '三号楼', '男宿舍', 3);
INSERT INTO `dorm_build` VALUES (4, '四号楼', '女宿舍', 4);

-- ----------------------------
-- Table structure for dorm_manager
-- ----------------------------
DROP TABLE IF EXISTS `dorm_manager`;
CREATE TABLE `dorm_manager`  (
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '123456' COMMENT '密码',
  `dormbuild_id` int(11) NOT NULL COMMENT '所管理的宿舍楼栋号',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '名字',
  `gender` enum('男','女') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '男' COMMENT '性别',
  `age` int(11) NOT NULL COMMENT '年龄',
  `phone_num` varchar(11) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dorm_manager
-- ----------------------------
INSERT INTO `dorm_manager` VALUES ('dorm1', '123456', 1, '张三', '男', 35, '15222223333', '12@email.com', NULL);
INSERT INTO `dorm_manager` VALUES ('dorm2', '123456', 2, '李四', '女', 55, '15333332222', NULL, NULL);
INSERT INTO `dorm_manager` VALUES ('dorm3', '123456', 3, '王五', '男', 38, '15855552222', NULL, NULL);
INSERT INTO `dorm_manager` VALUES ('dorm4', '123456', 4, '赵花', '女', 40, '15877776666', NULL, NULL);
INSERT INTO `dorm_manager` VALUES ('Mtest', '123456', 2, '宿管测试', '男', 22, '15899999999', NULL, NULL);

-- ----------------------------
-- Table structure for dorm_room
-- ----------------------------
DROP TABLE IF EXISTS `dorm_room`;
CREATE TABLE `dorm_room`  (
  `dormroom_id` int(11) NOT NULL COMMENT '宿舍房间号',
  `dormbuild_id` int(11) NOT NULL COMMENT '宿舍楼号',
  `floor_num` int(11) NOT NULL COMMENT '楼层',
  `max_capacity` int(11) NOT NULL DEFAULT 4 COMMENT '房间最大入住人数',
  `current_capacity` int(11) NOT NULL DEFAULT 0 COMMENT '当前房间入住人数',
  `first_bed` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '一号床位',
  `second_bed` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '二号床位',
  `third_bed` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '三号床位',
  `fourth_bed` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '四号床位',
  PRIMARY KEY (`dormroom_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dorm_room
-- ----------------------------
INSERT INTO `dorm_room` VALUES (1101, 1, 1, 4, 2, 'stu22', NULL, NULL, 'stu4');
INSERT INTO `dorm_room` VALUES (1103, 1, 1, 4, 4, 'stu8', 'stu9', 'stu10', 'stu11');
INSERT INTO `dorm_room` VALUES (1104, 1, 1, 4, 2, 'stu2', 'stu3', NULL, NULL);
INSERT INTO `dorm_room` VALUES (1201, 1, 2, 3, 2, 'stu1', 'stu5', NULL, NULL);
INSERT INTO `dorm_room` VALUES (2101, 2, 1, 4, 3, 'stu12', 'stu13', 'stu14', NULL);
INSERT INTO `dorm_room` VALUES (3101, 3, 1, 4, 3, 'stu15', 'stu16', 'stu16', NULL);
INSERT INTO `dorm_room` VALUES (4102, 4, 1, 4, 3, 'stu17', 'stu18', 'stu19', NULL);

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主题',
  `content` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '内容',
  `author` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '作者',
  `release_time` datetime NOT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (1, '入冬提醒', '<p>近期我校所在地区天气逐渐降低，同学们要注意多穿衣服，少熬夜，避免感染风寒！</p>', '大强', '2023-05-01 00:17:27');
INSERT INTO `notice` VALUES (2, '关于宿舍卫生的新规定', '<p>学生公寓是学生们主要的生活区域，兼具休息、学习、交际等多种功能，是培养、提升学生全面素质不可或缺的重要阵地。为了培养学生良好的行为素养和生活习惯，我们实行宿舍长内务准军事化管理，切实把学生公寓建成学生自我教育，自我管理和自我服务的家园。</p>', '张三', '2023-02-14 00:02:59');

-- ----------------------------
-- Table structure for repair
-- ----------------------------
DROP TABLE IF EXISTS `repair`;
CREATE TABLE `repair`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '订单编号',
  `repairer` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '报修人',
  `dormbuild_id` int(11) NOT NULL COMMENT '报修宿舍楼',
  `dormroom_id` int(11) NOT NULL COMMENT '报修宿舍房间号',
  `title` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '表单标题',
  `content` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '表单内容',
  `state` enum('完成','未完成') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '未完成' COMMENT '订单状态（是否维修完成）',
  `order_buildtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '订单创建时间',
  `order_finishtime` datetime NULL DEFAULT NULL COMMENT '订单完成时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of repair
-- ----------------------------
INSERT INTO `repair` VALUES (-12455934, '张三', 1, 1101, '阳台漏水', '阳台使用时会漏水请来修理', '未完成', '2023-03-14 20:37:35', NULL);
INSERT INTO `repair` VALUES (1, '强强', 1, 1101, '水龙头损坏', '水龙头损坏，请来1-1101宿舍修理', '完成', '2023-01-11 22:52:24', '2023-02-17 14:35:02');

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '学号',
  `password` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '123456' COMMENT '密码',
  `age` int(10) UNSIGNED NOT NULL COMMENT '年龄',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '姓名',
  `gender` enum('男','女') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '男' COMMENT '性别',
  `phone_num` varchar(11) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`) USING BTREE,
  UNIQUE INDEX `stu_num`(`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES ('Stest', '123456', 20, '学生测试', '男', '13233332222', NULL, NULL);
INSERT INTO `student` VALUES ('stu1', '123456', 18, '张三', '男', '15833332222', '123@qq.com', 'ab94d5ac984b4431bfbbf3f7d4bf14ef.jpg');
INSERT INTO `student` VALUES ('stu10', '123456', 19, '马克', '女', '15833333333', NULL, NULL);
INSERT INTO `student` VALUES ('stu11', '123456', 16, '巧巧', '女', '18922223333', NULL, NULL);
INSERT INTO `student` VALUES ('stu12', '123456', 17, '丽丽', '女', '17922222222', NULL, NULL);
INSERT INTO `student` VALUES ('stu13', '123456', 18, '美美', '女', '15822222222', NULL, NULL);
INSERT INTO `student` VALUES ('stu14', '123456', 20, '拉拉', '女', '13355556666', NULL, NULL);
INSERT INTO `student` VALUES ('stu15', '123456', 18, '贝贝', '男', '15899999999', NULL, NULL);
INSERT INTO `student` VALUES ('stu16', '123456', 18, '力力', '男', '14596475257', NULL, NULL);
INSERT INTO `student` VALUES ('stu17', '123456', 18, '阿成', '男', '15896542147', NULL, NULL);
INSERT INTO `student` VALUES ('stu18', '123456', 19, '阿达', '女', '14785635874', 'akk@akkmail.com', NULL);
INSERT INTO `student` VALUES ('stu19', '123456', 19, '帕森斯', '男', '15889658475', NULL, NULL);
INSERT INTO `student` VALUES ('stu2', '123456', 18, '田田', '男', '15875359641', NULL, NULL);
INSERT INTO `student` VALUES ('stu20', '123456', 21, '柠檬', '男', '15874563558', NULL, NULL);
INSERT INTO `student` VALUES ('stu21', '123456', 21, '面对', '男', '15889635874', NULL, NULL);
INSERT INTO `student` VALUES ('stu22', '123456', 25, '等等', '男', '13412341234', 'akkk@kkk.com', NULL);
INSERT INTO `student` VALUES ('stu3', '123456', 18, '吉安', '男', '15798657350', NULL, NULL);
INSERT INTO `student` VALUES ('stu4', '123456', 22, '力力', '男', '15878965874', NULL, NULL);
INSERT INTO `student` VALUES ('stu5', '123456', 19, '哦哦', '男', '15897535478', NULL, NULL);
INSERT INTO `student` VALUES ('stu6', '123456', 18, '泡泡', '男', '18987554765', NULL, NULL);
INSERT INTO `student` VALUES ('stu7', '123456', 15, '刚刚', '男', '15897543854', NULL, NULL);
INSERT INTO `student` VALUES ('stu8', '123456', 18, '七七', '男', '12332143215', NULL, NULL);
INSERT INTO `student` VALUES ('stu9', '123456', 20, '德萨', '男', '15889658741', NULL, NULL);

-- ----------------------------
-- Table structure for visitor
-- ----------------------------
DROP TABLE IF EXISTS `visitor`;
CREATE TABLE `visitor`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '姓名',
  `gender` enum('男','女') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '男' COMMENT '性别',
  `phone_num` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '电话',
  `origin_city` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '来源城市',
  `visit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '来访时间',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '事情',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of visitor
-- ----------------------------
INSERT INTO `visitor` VALUES (1, '张三', '男', '13433333333', '武汉', '2023-04-29 17:34:52', '探访孩子');
INSERT INTO `visitor` VALUES (2, '李四', '女', '15722222222', '江苏', '2023-05-01 17:08:06', '运送快递');
INSERT INTO `visitor` VALUES (3, '啊啊', '女', '13255555555', '湖北', '2023-03-25 16:41:21', '运送食品');

SET FOREIGN_KEY_CHECKS = 1;
