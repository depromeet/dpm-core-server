-- 2026-10-20 / 로그인 계정 연결 및 해제
-- 목적: 이메일이 같은 소셜 회원을 구분하고, 계정 연결 요청을 일회용으로 보관한다.
-- 선행: members, member_oauth, member_credentials. MySQL 8.
-- 후행: VERIFY 확인 후 이메일 자동 연결을 제거한 앱을 배포한다.
-- 주의: 가입/로그인/계정 변경 쓰기를 중지하고 백업 후 실행한다.
--       DDL은 암묵적으로 COMMIT된다. 트랜잭션으로 전체 롤백되지 않는다.
--       기존 회원/연결/자격 증명 행은 수정하지 않는다. 이메일 로그인의 UNIQUE는 유지한다.
--       DB별로 다른 기존 인덱스 이름은 실제 메타데이터에서 찾는다.
--       IF NOT EXISTS는 잘못된 기존 테이블을 고치지 않으므로 VERIFY를 반드시 확인한다.
--       실패하면 앱을 배포하지 말고 원인 해결 후 재실행한다.

-- [1] 연결 요청: 인증 코드나 제공자 액세스 토큰을 저장하지 않는다.
-- state는 해시로 저장하고 PKCE verifier는 요청 유효기간 동안만 보관한다.
START TRANSACTION;
CREATE TABLE IF NOT EXISTS member_oauth_link_requests (
    state_hash VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    member_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    redirect_uri VARCHAR(2048) NOT NULL,
    code_verifier VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (state_hash),
    INDEX idx_member_oauth_link_requests_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
COMMIT;

-- [2] 제공자의 opaque 계정 ID는 대소문자를 구분한다.
SET @account_sql = IF(
    (SELECT COLLATION_NAME FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='member_oauth' AND COLUMN_NAME='external_id') = 'utf8mb4_bin',
    'SELECT 1',
    'ALTER TABLE member_oauth MODIFY COLUMN external_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL'
);
PREPARE account_statement FROM @account_sql;
EXECUTE account_statement;
DEALLOCATE PREPARE account_statement;

-- 새 UNIQUE를 먼저 만든다. 중복이 존재하면 여기서 실패하며 기존 UNIQUE를 제거하지 않는다.
SET @account_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='member_oauth'
             AND INDEX_NAME='uk_member_oauth_provider_external'),
    'SELECT 1',
    'ALTER TABLE member_oauth ADD CONSTRAINT uk_member_oauth_provider_external UNIQUE (provider, external_id)'
);
PREPARE account_statement FROM @account_sql;
EXECUTE account_statement;
DEALLOCATE PREPARE account_statement;

SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(index_name, '`', '``'), '`') SEPARATOR ', ')
INTO @account_drop_indexes
FROM (
    SELECT INDEX_NAME AS index_name
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='member_oauth'
      AND NON_UNIQUE=0 AND INDEX_NAME<>'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING COUNT(*)=1 AND MAX(COLUMN_NAME)='external_id'
) old_indexes;
SET @account_sql = IF(@account_drop_indexes IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE member_oauth ', @account_drop_indexes));
PREPARE account_statement FROM @account_sql;
EXECUTE account_statement;
DEALLOCATE PREPARE account_statement;

-- [3] 가입/연락 이메일은 로그인 수단의 소유 증명이 아니다. 조회용 인덱스를 먼저 유지한다.
SET @account_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='members' AND INDEX_NAME='idx_members_signup_email'),
    'SELECT 1',
    'ALTER TABLE members ADD INDEX idx_members_signup_email (signup_email)'
);
PREPARE account_statement FROM @account_sql;
EXECUTE account_statement;
DEALLOCATE PREPARE account_statement;

SET @account_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='members' AND INDEX_NAME='idx_members_email'),
    'SELECT 1',
    'ALTER TABLE members ADD INDEX idx_members_email (email)'
);
PREPARE account_statement FROM @account_sql;
EXECUTE account_statement;
DEALLOCATE PREPARE account_statement;

SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(index_name, '`', '``'), '`') SEPARATOR ', ')
INTO @account_drop_indexes
FROM (
    SELECT INDEX_NAME AS index_name
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='members'
      AND NON_UNIQUE=0 AND INDEX_NAME<>'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING COUNT(*)=1 AND MAX(COLUMN_NAME) IN ('email','signup_email')
) old_indexes;
SET @account_sql = IF(@account_drop_indexes IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE members ', @account_drop_indexes));
PREPARE account_statement FROM @account_sql;
EXECUTE account_statement;
DEALLOCATE PREPARE account_statement;

-- [4] VERIFY (읽기 전용)
-- 6개 컬럼, state_hash PK, expires_at 인덱스. 타입/NULL 허용 여부를 위 정의와 비교한다.
SHOW CREATE TABLE member_oauth_link_requests;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='member_oauth'
  AND COLUMN_NAME='external_id';

-- members 이메일 2개는 NON_UNIQUE=1, OAuth는 (provider,external_id) UNIQUE.
-- member_credentials.email UNIQUE가 반드시 남아 있어야 한다.
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS indexed_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('members','member_oauth','member_credentials','member_oauth_link_requests')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ROLLBACK: 새 코드의 쓰기를 중지한 뒤 판단한다.
-- 중복 이메일 가입 후 이전 UNIQUE를 바로 복원할 수 없으므로 자동 롤백/회원 삭제를 하지 않는다.
-- 백업 및 이후 가입 내역을 대조하여 복구 계획을 세우고, 이전 자동 연결 코드로 바로 되돌리지 않는다.
