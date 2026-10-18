-- 2026-10-18 / 공지에 cohort_id 추가
-- 목적: 공지를 기수별로 구분해 조회/알림 범위를 활성 기수로 제한한다.
-- 선행: announcements 테이블, cohorts 테이블. 쓰기 중지는 필요 없으나 적용 중 공지 생성은 잠시 멈춘다.
-- 후행: 이 SQL과 VERIFY를 완주한 다음 앱을 배포한다. 엔티티 ddl-auto=validate는 컬럼을 만들지 않는다.
-- 검증: 백필 전/후 NULL 수, 18기 cohort_id 일치, NOT NULL 전환 성공, FK/인덱스 존재.
-- 주의: MySQL DDL은 암묵적으로 COMMIT된다. 재실행 시 기존 백필 행은 UPDATE 대상에서 제외된다.

START TRANSACTION;

-- [1] cohort_id 컬럼이 없으면 NULL 허용으로 추가한다.
SET @col_missing = (
    SELECT COUNT(*) = 0 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND COLUMN_NAME = 'cohort_id'
);
SET @add_sql = IF(@col_missing,
    'ALTER TABLE announcements ADD COLUMN cohort_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @add_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

COMMIT;

-- [2] 기존 공지는 18기(cohorts.value = '18')로 백필한다.
START TRANSACTION;

SET @cohort_18_id = (SELECT cohort_id FROM cohorts WHERE value = '18');

UPDATE announcements
SET cohort_id = @cohort_18_id
WHERE cohort_id IS NULL
  AND @cohort_18_id IS NOT NULL;

COMMIT;

-- [3] NOT NULL 로 전환한다.
START TRANSACTION;

SET @col_nullable = (
    SELECT IS_NULLABLE = 'YES' FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND COLUMN_NAME = 'cohort_id'
);
SET @modify_sql = IF(@col_nullable,
    'ALTER TABLE announcements MODIFY COLUMN cohort_id BIGINT NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @modify_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

COMMIT;

-- [4] 조회 성능을 위한 인덱스를 추가한다.
START TRANSACTION;

SET @idx_missing = (
    SELECT COUNT(*) = 0 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND INDEX_NAME = 'idx_announcements_cohort_id'
);
SET @idx_sql = IF(@idx_missing,
    'CREATE INDEX idx_announcements_cohort_id ON announcements (cohort_id)',
    'SELECT 1');
PREPARE stmt FROM @idx_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

COMMIT;

-- VERIFY (읽기 전용)
-- 1. cohort_id가 NULL인 행이 없어야 한다.
SELECT COUNT(*) AS null_count FROM announcements WHERE cohort_id IS NULL;
-- 2. 각 기수별 공지 개수 (기존은 전부 18기여야 함).
SELECT c.value, COUNT(*) AS announcement_count
FROM announcements a
JOIN cohorts c ON c.cohort_id = a.cohort_id
GROUP BY c.value
ORDER BY c.value;
-- 3. 컬럼 메타데이터 확인.
SELECT COLUMN_NAME, IS_NULLABLE, DATA_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND COLUMN_NAME = 'cohort_id';
-- 4. 인덱스 확인.
SELECT INDEX_NAME, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND INDEX_NAME = 'idx_announcements_cohort_id';
