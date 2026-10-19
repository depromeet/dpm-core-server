-- 2026-10-09 / 최초 프로필 입력 완료 표시
-- 목적: 기존 정상 한글 이름/파트는 유지하고 나머지 회원에게 이름과 파트 최초 입력을 요구한다.
-- 선행: members.name VARCHAR(255), members.part 및 MySQL 8. 적용 중 가입/회원 쓰기를 잠시 중지한다.
-- 후행: 이 SQL과 VERIFY를 완주한 다음 앱을 배포한다. 엔티티의 ddl-auto=validate는 컬럼을 만들지 않는다.
-- 검증: 정상/누락/비한글 기존 회원, 완료 후 재실행, ADD 직후 및 DEFAULT 변경 직후 중단 복구.
-- 주의: MySQL DDL은 암묵적으로 COMMIT된다. 임시 sentinel은 기존 행을 구분하며 완료 전 앱을 배포하지 않는다.
--       재실행은 sentinel 행만 처리하므로 배포 후 만들어진 NULL 행을 완료 처리하지 않는다.
--       한글 서식 검사이며 실명 인증이 아니다. 이름/파트 원본은 변경하지 않는다.

-- [1] 기존 행을 표시한 뒤 신규 행의 기본값을 NULL로 변경한다.
START TRANSACTION;
SET @profile_column_missing = (
    SELECT COUNT(*) = 0 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND COLUMN_NAME = 'profile_completed_at'
);
SET @profile_add_sql = IF(@profile_column_missing,
    'ALTER TABLE members ADD COLUMN profile_completed_at DATETIME(6) NULL DEFAULT ''1970-01-02 00:00:00.000000''',
    'SELECT 1');
PREPARE profile_statement FROM @profile_add_sql;
EXECUTE profile_statement;
DEALLOCATE PREPARE profile_statement;
ALTER TABLE members ALTER COLUMN profile_completed_at SET DEFAULT NULL;
COMMIT;

-- [2] sentinel 행만 한 번 분류한다. 중단되면 같은 SQL 재실행으로 복구할 수 있다.
START TRANSACTION;
UPDATE members
SET profile_completed_at = CASE
    WHEN deleted_at IS NULL AND status <> 'WITHDRAWN'
        AND CHAR_LENGTH(REGEXP_REPLACE(name, '^[[:space:]]+|[[:space:]]+$', '')) BETWEEN 1 AND 255
        AND REGEXP_LIKE(REGEXP_REPLACE(name, '^[[:space:]]+|[[:space:]]+$', ''), '^[가-힣]+( +[가-힣]+)*$', 'c')
        AND BINARY part IN ('WEB', 'SERVER', 'DESIGN', 'IOS', 'ANDROID')
    THEN UTC_TIMESTAMP(6)
    ELSE NULL
END
WHERE profile_completed_at = '1970-01-02 00:00:00.000000';
COMMIT;

-- [3] VERIFY: DATETIME(6), nullable YES, default NULL / sentinel 0건이어야 한다.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND COLUMN_NAME = 'profile_completed_at';
SELECT COUNT(*) AS unfinished_migration_rows FROM members
WHERE profile_completed_at = '1970-01-02 00:00:00.000000';
