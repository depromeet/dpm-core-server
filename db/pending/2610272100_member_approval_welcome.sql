-- 2026-10-27 / 회원별 승인 완료 화면 확인
-- 목적: 승인 완료 화면 확인 여부를 기기가 아닌 회원 기준으로 저장한다.
-- 선행: MySQL 8, members. 회원 생성/승인 쓰기를 중지하고 백업한다.
-- 후행: VERIFY 완료 후 앱을 배포하고 회원 쓰기를 재개한다.
-- 검증: 기존 행 true, 신규 행 false, 재실행 시 기존 true/false 불변. BOOLEAN NOT NULL DEFAULT false.
-- 주의: DDL은 암묵적으로 COMMIT된다. ADD 직후 중단되어도 재실행은 DEFAULT만 보정한다.
--       기존 미승인 행의 true는 승인 시 false로 초기화되며 미승인 조회에서는 false를 반환한다.

-- [1] 기존 회원만 확인 완료로 초기화한 뒤 신규 회원의 기본값을 false로 변경한다.
START TRANSACTION;
SET @welcome_column_missing = (
    SELECT COUNT(*) = 0 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND COLUMN_NAME = 'approval_welcome_seen'
);
SET @welcome_add_sql = IF(@welcome_column_missing,
    'ALTER TABLE members ADD COLUMN approval_welcome_seen BOOLEAN NOT NULL DEFAULT TRUE',
    'SELECT 1');
PREPARE welcome_statement FROM @welcome_add_sql;
EXECUTE welcome_statement;
DEALLOCATE PREPARE welcome_statement;
ALTER TABLE members ALTER COLUMN approval_welcome_seen SET DEFAULT FALSE;
COMMIT;

-- [2] VERIFY: tinyint(1), nullable NO, default 0 / invalid_rows 0건.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND COLUMN_NAME = 'approval_welcome_seen';
SELECT COUNT(*) AS invalid_rows FROM members
WHERE approval_welcome_seen IS NULL OR approval_welcome_seen NOT IN (0, 1);
