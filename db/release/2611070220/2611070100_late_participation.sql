-- 2026-11-07 · 늦은 참여를 출석 코드로 확인한 시각 (#725)
-- 목적: 기존 결석 기록은 그대로 두고, 도입 이후 확인된 늦은 참여만 구분한다.
-- 선행: MySQL 8 CLI에서 단독 실행. attendances ALTER 및 CREATE ROUTINE, ALTER ROUTINE, EXECUTE 권한 필요.
-- 후행: VERIFY 확인 후 앱 배포. DDL은 암묵 커밋되며 기존 행은 NULL로 남긴다.

-- [1] 컬럼 추가 및 재실행 시 정의 검증
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_late_participation_2611070100$$
CREATE PROCEDURE migrate_late_participation_2611070100()
SQL SECURITY INVOKER
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'attendances' AND column_name = 'late_participated_at'
    ) THEN
        ALTER TABLE attendances ADD COLUMN late_participated_at TIMESTAMP(6) NULL DEFAULT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'attendances' AND column_name = 'late_participated_at'
          AND column_type = 'timestamp(6)' AND is_nullable = 'YES' AND column_default IS NULL AND extra = ''
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected attendances.late_participated_at TIMESTAMP(6) NULL DEFAULT NULL';
    END IF;
END$$
CALL migrate_late_participation_2611070100()$$
DROP PROCEDURE migrate_late_participation_2611070100$$
DELIMITER ;

-- [2] VERIFY
SHOW COLUMNS FROM attendances WHERE Field = 'late_participated_at';
