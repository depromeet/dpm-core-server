-- =============================================================================
-- 2026-09-24 · member_oauth.email 컬럼 추가
-- =============================================================================
-- 목적: 로그인 수단(OAuth 제공자)별 이메일을 저장한다. (#563)
--       1) member_oauth.email 컬럼 추가 (NULL 허용)
--       2) 기존 연동 행을 members.signup_email 로 채우기
-- 선행: 없음
-- 후행: 없음
-- 검증: 파일 하단 VERIFY 섹션 (읽기 전용)
-- 주의: 애플리케이션이 ddl-auto: validate 이므로 이 스크립트를 먼저 적용한 뒤 배포해야 한다.
--       컬럼이 없으면 MemberOAuthEntity 스키마 검증에 실패해 서버가 기동되지 않는다.

-- -----------------------------------------------------------------------------
-- [1] member_oauth.email 컬럼 추가
-- -----------------------------------------------------------------------------
START TRANSACTION;

SET @has_email := (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema=DATABASE() AND table_name='member_oauth' AND column_name='email');
SET @sql1 := IF(@has_email=0,
    'ALTER TABLE member_oauth ADD COLUMN email VARCHAR(255) NULL COMMENT ''OAuth 제공자가 내려준 이메일 (로그인 시 최신화)''',
    'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

COMMIT;

-- -----------------------------------------------------------------------------
-- [2] 기존 연동 행 이메일 채우기
--     소셜 계정은 signup_email 과 제공자 이메일이 같은 회원에게만 연동되므로
--     (Kakao/Apple 로그인 시 findAllBySignupEmail 로 매칭) 연동 시점의 이메일은
--     signup_email 과 같다. 이후 제공자 쪽에서 이메일이 바뀐 경우는 다음 로그인 때
--     애플리케이션이 최신값으로 갱신한다.
--     탈퇴로 익명화된 회원(withdrawn+...@withdrawn.local)은 채우지 않는다.
-- -----------------------------------------------------------------------------
START TRANSACTION;

UPDATE member_oauth mo
INNER JOIN members m ON mo.member_id = m.member_id
SET mo.email = m.signup_email
WHERE mo.email IS NULL
  AND m.signup_email NOT LIKE '%@withdrawn.local';

COMMIT;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
-- 컬럼 존재 (기대: email / varchar(255) / YES)
SHOW COLUMNS FROM member_oauth LIKE 'email';

-- 채움 현황 (기대: null_email 은 탈퇴 회원 또는 members 에 없는 고아 행 수와 일치)
SELECT provider,
       COUNT(*)                  AS total,
       COUNT(email)              AS with_email,
       COUNT(*) - COUNT(email)   AS null_email
FROM member_oauth
GROUP BY provider;

-- 채우지 못한 행의 사유 (기대: withdrawn 또는 orphan 만)
SELECT mo.member_oauth_id,
       mo.provider,
       CASE
           WHEN m.member_id IS NULL THEN 'orphan'
           WHEN m.signup_email LIKE '%@withdrawn.local' THEN 'withdrawn'
           ELSE 'unexpected'
       END AS reason
FROM member_oauth mo
LEFT JOIN members m ON mo.member_id = m.member_id
WHERE mo.email IS NULL;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 컬럼 추가 + 값 채우기만 수행하므로 컬럼 DROP 으로 충분.
-- 단, 롤백 전에 이 컬럼을 사용하는 애플리케이션 버전을 먼저 내려야 한다.
-- (ddl-auto: validate 로 인해 컬럼이 없으면 기동 실패)
--
--  START TRANSACTION;
--  ALTER TABLE member_oauth DROP COLUMN email;
--  COMMIT;
-- =============================================================================
