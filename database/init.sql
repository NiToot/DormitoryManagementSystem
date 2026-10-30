-- Local demonstration database reconstructed from the current entities/mappers.
-- This is not a migration or a copy of the original author's database.
-- Re-running preserves existing rows. Use only a dedicated demonstration schema.
CREATE DATABASE IF NOT EXISTS dormitory_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dormitory_demo;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS accommodation_lock (id INT PRIMARY KEY) ENGINE=InnoDB;
INSERT IGNORE INTO accommodation_lock(id) VALUES(1);

CREATE TABLE IF NOT EXISTS admin (
  username VARCHAR(32) PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  auth_version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(64) NOT NULL,
  gender VARCHAR(8), age INT NOT NULL DEFAULT 0,
  phone_num VARCHAR(32), email VARCHAR(128), avatar VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student (
  username VARCHAR(32) PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  auth_version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(64) NOT NULL,
  age INT NOT NULL DEFAULT 0, gender VARCHAR(8),
  phone_num VARCHAR(32), email VARCHAR(128), avatar VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dorm_build (
  id INT PRIMARY KEY AUTO_INCREMENT,
  dormbuild_id INT NOT NULL,
  dormbuild_name VARCHAR(64) NOT NULL,
  dormbuild_detail VARCHAR(255),
  UNIQUE KEY uk_build_number (dormbuild_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dorm_manager (
  username VARCHAR(32) PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  auth_version BIGINT NOT NULL DEFAULT 0,
  dormbuild_id INT NOT NULL,
  name VARCHAR(64) NOT NULL,
  gender VARCHAR(8), age INT NOT NULL DEFAULT 0,
  phone_num VARCHAR(32), email VARCHAR(128), avatar VARCHAR(255),
  KEY idx_manager_build (dormbuild_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dorm_room (
  version BIGINT NOT NULL DEFAULT 0,
  dormroom_id INT PRIMARY KEY,
  dormbuild_id INT NOT NULL, floor_num INT NOT NULL,
  max_capacity INT NOT NULL DEFAULT 4,
  current_capacity INT NOT NULL DEFAULT 0,
  first_bed VARCHAR(32), second_bed VARCHAR(32),
  third_bed VARCHAR(32), fourth_bed VARCHAR(32),
  KEY idx_room_build (dormbuild_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notice (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL, content MEDIUMTEXT,
  author VARCHAR(64), release_time VARCHAR(32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS repair (
  id INT PRIMARY KEY AUTO_INCREMENT,
  repairer VARCHAR(64) NOT NULL,
  student_username VARCHAR(32),
  dormbuild_id INT NOT NULL, dormroom_id INT NOT NULL,
  title VARCHAR(200) NOT NULL, content MEDIUMTEXT,
  state VARCHAR(16) NOT NULL DEFAULT '未处理',
  order_buildtime VARCHAR(32), order_finishtime VARCHAR(32),
  KEY idx_repair_person (repairer),
  KEY idx_repair_owner (student_username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS adjust_room (
  version BIGINT NOT NULL DEFAULT 0,
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(32) NOT NULL, name VARCHAR(64) NOT NULL,
  currentroom_id INT NOT NULL, currentbed_id INT NOT NULL,
  towardsroom_id INT NOT NULL, towardsbed_id INT NOT NULL,
  state VARCHAR(16) NOT NULL DEFAULT '未处理',
  apply_time VARCHAR(32), finish_time VARCHAR(32),
  KEY idx_adjust_student (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS visitor (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL, gender VARCHAR(8),
  phone_num VARCHAR(32), origin_city VARCHAR(128),
  visit_time VARCHAR(32), content TEXT, dormbuild_id INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Public, fictional demo accounts. BCrypt cost 12; demo password: Demo123456.
INSERT INTO admin (username,password,name,gender,age,email)
SELECT 'admin01','$2a$12$RW/955z9wB84CAhYyTrlVerztLItJdTHRGTCFToU.zFvRVqu6REjy','演示管理员','男',30,'admin@example.invalid'
WHERE NOT EXISTS (SELECT 1 FROM admin WHERE username='admin01');
INSERT INTO student (username,password,name,age,gender,email)
SELECT '20230001','$2a$12$RW/955z9wB84CAhYyTrlVerztLItJdTHRGTCFToU.zFvRVqu6REjy','演示学生甲',20,'男','student1@example.invalid'
WHERE NOT EXISTS (SELECT 1 FROM student WHERE username='20230001');
INSERT INTO student (username,password,name,age,gender,email)
SELECT '20230002','$2a$12$RW/955z9wB84CAhYyTrlVerztLItJdTHRGTCFToU.zFvRVqu6REjy','演示学生乙',20,'男','student2@example.invalid'
WHERE NOT EXISTS (SELECT 1 FROM student WHERE username='20230002');
INSERT INTO student (username,password,name,age,gender,email)
SELECT '20230003','$2a$12$RW/955z9wB84CAhYyTrlVerztLItJdTHRGTCFToU.zFvRVqu6REjy','演示学生丙',20,'女','student3@example.invalid'
WHERE NOT EXISTS (SELECT 1 FROM student WHERE username='20230003');

INSERT INTO dorm_build (dormbuild_id,dormbuild_name,dormbuild_detail)
SELECT 1,'一号楼','本地演示楼宇' WHERE NOT EXISTS (SELECT 1 FROM dorm_build WHERE dormbuild_id=1);
INSERT INTO dorm_build (dormbuild_id,dormbuild_name,dormbuild_detail)
SELECT 2,'二号楼','本地演示楼宇' WHERE NOT EXISTS (SELECT 1 FROM dorm_build WHERE dormbuild_id=2);
INSERT INTO dorm_build (dormbuild_id,dormbuild_name,dormbuild_detail)
SELECT 3,'三号楼','本地演示楼宇' WHERE NOT EXISTS (SELECT 1 FROM dorm_build WHERE dormbuild_id=3);
INSERT INTO dorm_build (dormbuild_id,dormbuild_name,dormbuild_detail)
SELECT 4,'四号楼','本地演示楼宇' WHERE NOT EXISTS (SELECT 1 FROM dorm_build WHERE dormbuild_id=4);
INSERT INTO dorm_manager (username,password,dormbuild_id,name,gender,age,email)
SELECT 'dorm01','$2a$12$RW/955z9wB84CAhYyTrlVerztLItJdTHRGTCFToU.zFvRVqu6REjy',1,'演示宿管','女',35,'manager@example.invalid'
WHERE NOT EXISTS (SELECT 1 FROM dorm_manager WHERE username='dorm01');

INSERT INTO dorm_room (dormroom_id,dormbuild_id,floor_num,max_capacity,current_capacity,first_bed,second_bed)
SELECT 1001,1,1,4,2,'20230001','20230002' WHERE NOT EXISTS (SELECT 1 FROM dorm_room WHERE dormroom_id=1001);
INSERT INTO dorm_room (dormroom_id,dormbuild_id,floor_num,max_capacity,current_capacity)
SELECT 1002,1,1,4,0 WHERE NOT EXISTS (SELECT 1 FROM dorm_room WHERE dormroom_id=1002);
INSERT INTO dorm_room (dormroom_id,dormbuild_id,floor_num,max_capacity,current_capacity)
SELECT 2001,2,1,4,0 WHERE NOT EXISTS (SELECT 1 FROM dorm_room WHERE dormroom_id=2001);
INSERT INTO dorm_room (dormroom_id,dormbuild_id,floor_num,max_capacity,current_capacity)
SELECT 3001,3,1,4,0 WHERE NOT EXISTS (SELECT 1 FROM dorm_room WHERE dormroom_id=3001);
INSERT INTO dorm_room (dormroom_id,dormbuild_id,floor_num,max_capacity,current_capacity)
SELECT 4001,4,1,4,0 WHERE NOT EXISTS (SELECT 1 FROM dorm_room WHERE dormroom_id=4001);

INSERT INTO notice (id,title,content,author,release_time)
SELECT 1,'欢迎使用宿舍管理演示系统','<p>这是本地虚构演示数据，请勿录入真实个人信息。</p>','演示管理员','2026-10-30 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE id=1);
INSERT INTO repair (id,repairer,student_username,dormbuild_id,dormroom_id,title,content,state,order_buildtime)
SELECT 1,'演示学生甲','20230001',1,1001,'演示报修','<p>演示：照明设备检查。</p>','未处理','2026-10-30 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM repair WHERE id=1);
INSERT INTO adjust_room (id,username,name,currentroom_id,currentbed_id,towardsroom_id,towardsbed_id,state,apply_time)
SELECT 1,'20230001','演示学生甲',1001,1,1002,1,'未处理','2026-10-30 09:00:00'
WHERE NOT EXISTS (SELECT 1 FROM adjust_room WHERE id=1);
INSERT INTO visitor (id,name,gender,origin_city,visit_time,content,dormbuild_id)
SELECT 1,'演示访客','男','演示城市','2026-10-30 09:00:00','本地演示来访记录',1
WHERE NOT EXISTS (SELECT 1 FROM visitor WHERE id=1);
