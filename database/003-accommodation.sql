-- Stop the old backend and back up the dedicated demo schema before running.
USE dormitory_demo;
CREATE TABLE IF NOT EXISTS accommodation_lock (id INT PRIMARY KEY) ENGINE=InnoDB;
INSERT IGNORE INTO accommodation_lock(id) VALUES(1);
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='dorm_room' AND column_name='version'),'SELECT 1','ALTER TABLE dorm_room ADD COLUMN version BIGINT NOT NULL DEFAULT 0');
PREPARE migration FROM @ddl; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='adjust_room' AND column_name='version'),'SELECT 1','ALTER TABLE adjust_room ADD COLUMN version BIGINT NOT NULL DEFAULT 0');
PREPARE migration FROM @ddl; EXECUTE migration; DEALLOCATE PREPARE migration;
-- Diagnostic only: never delete or choose a winner for historical conflicts.
SELECT username,COUNT(*) AS occupied_beds FROM (
 SELECT first_bed username FROM dorm_room UNION ALL SELECT second_bed FROM dorm_room
 UNION ALL SELECT third_bed FROM dorm_room UNION ALL SELECT fourth_bed FROM dorm_room
) beds WHERE username IS NOT NULL GROUP BY username HAVING COUNT(*)>1;
SELECT username,COUNT(*) AS pending_applications FROM adjust_room WHERE state='未处理' GROUP BY username HAVING COUNT(*)>1;
