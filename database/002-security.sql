-- Dedicated demonstration schema migration. No rows are deleted.
USE dormitory_demo;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='admin' AND column_name='auth_version'),'SELECT 1','ALTER TABLE admin ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0');
PREPARE migration FROM @ddl; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='student' AND column_name='auth_version'),'SELECT 1','ALTER TABLE student ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0');
PREPARE migration FROM @ddl; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='dorm_manager' AND column_name='auth_version'),'SELECT 1','ALTER TABLE dorm_manager ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0');
PREPARE migration FROM @ddl; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='repair' AND column_name='student_username'),'SELECT 1','ALTER TABLE repair ADD COLUMN student_username VARCHAR(32) NULL, ADD INDEX idx_repair_student (student_username)');
PREPARE migration FROM @ddl; EXECUTE migration; DEALLOCATE PREPARE migration;
SET @ddl=IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='visitor' AND column_name='dormbuild_id'),'SELECT 1','ALTER TABLE visitor ADD COLUMN dormbuild_id INT NULL');
PREPARE migration FROM @ddl; EXECUTE migration; DEALLOCATE PREPARE migration;
-- Ambiguous names remain unassigned and are visible only to administrators.
UPDATE repair r JOIN (SELECT name,MIN(username) username FROM student GROUP BY name HAVING COUNT(*)=1) s
ON r.repairer=s.name SET r.student_username=s.username WHERE r.student_username IS NULL;
-- Password hashing is explicit: start once with --app.security.migrate-legacy-passwords=true.
