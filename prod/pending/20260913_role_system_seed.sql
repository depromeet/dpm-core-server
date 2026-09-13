-- =============================================================================
-- 2026-09-13 · Phase 1 본작업: 역할·기수 시스템 시드 & 재매핑
-- =============================================================================
-- 목적: 문서 §2-2 / §7 반영. 아래 5개 원본 스크립트를 하나로 합침.
--       1) cohorts.is_active/activated_at 컬럼 + 활성 기수 세팅 + cohort_id=0 특수 슬롯
--       2) roles 시드 (MASTER/CORE/ORGANIZER/DEEPER/GUEST) + role_permissions 매트릭스
--       3) member_roles.role_id 를 "N기 역할" → canonical role 로 재매핑
--       4) member_cohorts 중복 제거 + UNIQUE(member_id, cohort_id)
--       5) 구시스템 member_authorities / authorities 아카이빙 후 DROP
-- 선행: 20260913_backup_member_roles.sql
-- 후행: 20260913_schema_alignment.sql
-- 검증: 파일 하단 VERIFY 섹션 (읽기 전용)

-- -----------------------------------------------------------------------------
-- [1] cohorts.is_active / activated_at + cohort_id=0 특수 슬롯
-- -----------------------------------------------------------------------------
SET @has_is_active := (SELECT COUNT(*) FROM information_schema.columns
                       WHERE table_schema=DATABASE() AND table_name='cohorts' AND column_name='is_active');
SET @sql1 := IF(@has_is_active=0,
    'ALTER TABLE cohorts ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @has_activated_at := (SELECT COUNT(*) FROM information_schema.columns
                          WHERE table_schema=DATABASE() AND table_name='cohorts' AND column_name='activated_at');
SET @sql2 := IF(@has_activated_at=0,
    'ALTER TABLE cohorts ADD COLUMN activated_at DATETIME NULL',
    'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

UPDATE cohorts SET is_active = TRUE, activated_at = NOW()
WHERE cohort_id = (SELECT cohort_id FROM (SELECT cohort_id FROM cohorts ORDER BY CAST(`value` AS UNSIGNED) DESC LIMIT 1) t);

-- 특수 슬롯 (§2-2): MASTER / GUEST role 저장 위치. is_active 는 항상 FALSE.
SET @prev_sql_mode := @@SESSION.sql_mode;
SET SESSION sql_mode = CONCAT_WS(',', @prev_sql_mode, 'NO_AUTO_VALUE_ON_ZERO');
INSERT INTO cohorts (cohort_id, `value`, is_active, activated_at, created_at, updated_at)
VALUES (0, '0', FALSE, NULL, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000)
ON DUPLICATE KEY UPDATE
    is_active    = FALSE,
    activated_at = NULL,
    updated_at   = UNIX_TIMESTAMP()*1000;
SET SESSION sql_mode = @prev_sql_mode;

-- -----------------------------------------------------------------------------
-- [2] roles 시드 + role_permissions 매트릭스
-- -----------------------------------------------------------------------------
START TRANSACTION;

INSERT INTO roles (name) VALUES ('MASTER'), ('CORE'), ('ORGANIZER'), ('DEEPER'), ('GUEST')
ON DUPLICATE KEY UPDATE name = VALUES(name);

DELETE rp FROM role_permissions rp
INNER JOIN roles r ON rp.role_id = r.role_id
WHERE r.name IN ('MASTER', 'CORE', 'ORGANIZER', 'DEEPER', 'GUEST') AND rp.revoked_at IS NULL;

-- MASTER: 최상위 super-admin. 문서 §6-1 결정에 따라 CORE 와 동일 매트릭스로 시작.
INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT (SELECT role_id FROM roles WHERE name = 'MASTER'), p.permission_id, NOW()
FROM permissions p WHERE (p.resource, p.action) IN (
    ('MEMBER','CREATE'),('MEMBER','READ'),('MEMBER','UPDATE'),('MEMBER','DELETE'),
    ('SESSION','CREATE'),('SESSION','READ'),('SESSION','UPDATE'),('SESSION','DELETE'),
    ('ATTENDANCE','CREATE'),('ATTENDANCE','READ'),('ATTENDANCE','UPDATE'),
    ('GATHERING','READ'),
    ('BILL','CREATE'),('BILL','READ'),('BILL','UPDATE'),
    ('AFTER_PARTY','CREATE'),('AFTER_PARTY','READ'),('AFTER_PARTY','UPDATE'),
    ('COHORT','CREATE'),('COHORT','UPDATE'),('COHORT','DELETE'),
    ('AUTHORIZATION','READ'),('AUTHORIZATION','UPDATE')
);

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT (SELECT role_id FROM roles WHERE name = 'CORE'), p.permission_id, NOW()
FROM permissions p WHERE (p.resource, p.action) IN (
    ('MEMBER','CREATE'),('MEMBER','READ'),('MEMBER','UPDATE'),('MEMBER','DELETE'),
    ('SESSION','CREATE'),('SESSION','READ'),('SESSION','UPDATE'),('SESSION','DELETE'),
    ('ATTENDANCE','CREATE'),('ATTENDANCE','READ'),('ATTENDANCE','UPDATE'),
    ('GATHERING','READ'),
    ('BILL','CREATE'),('BILL','READ'),('BILL','UPDATE'),
    ('AFTER_PARTY','CREATE'),('AFTER_PARTY','READ'),('AFTER_PARTY','UPDATE'),
    ('COHORT','CREATE'),('COHORT','UPDATE'),('COHORT','DELETE'),
    ('AUTHORIZATION','READ'),('AUTHORIZATION','UPDATE')
);

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT (SELECT role_id FROM roles WHERE name = 'ORGANIZER'), p.permission_id, NOW()
FROM permissions p WHERE (p.resource, p.action) IN (
    ('MEMBER','CREATE'),('MEMBER','READ'),('MEMBER','UPDATE'),
    ('SESSION','CREATE'),('SESSION','READ'),('SESSION','UPDATE'),('SESSION','DELETE'),
    ('ATTENDANCE','CREATE'),('ATTENDANCE','READ'),('ATTENDANCE','UPDATE'),
    ('GATHERING','READ'),
    ('BILL','CREATE'),('BILL','READ'),('BILL','UPDATE'),
    ('AFTER_PARTY','CREATE'),('AFTER_PARTY','READ'),('AFTER_PARTY','UPDATE'),
    ('AUTHORIZATION','READ')
);

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT (SELECT role_id FROM roles WHERE name = 'DEEPER'), p.permission_id, NOW()
FROM permissions p WHERE (p.resource, p.action) IN (
    ('MEMBER','READ'),('SESSION','READ'),
    ('ATTENDANCE','CREATE'),('ATTENDANCE','READ'),
    ('GATHERING','READ'),('BILL','READ'),('AFTER_PARTY','READ')
);

COMMIT;

-- -----------------------------------------------------------------------------
-- [3] member_roles 재매핑: "N기 운영진/디퍼/코어" → canonical role
-- -----------------------------------------------------------------------------
START TRANSACTION;

UPDATE member_roles mr INNER JOIN roles old_role ON mr.role_id = old_role.role_id
SET mr.role_id = (SELECT role_id FROM roles WHERE name = 'ORGANIZER')
WHERE old_role.name REGEXP '^[0-9]+기 운영진$';

UPDATE member_roles mr INNER JOIN roles old_role ON mr.role_id = old_role.role_id
SET mr.role_id = (SELECT role_id FROM roles WHERE name = 'DEEPER')
WHERE old_role.name REGEXP '^[0-9]+기 디퍼$';

UPDATE member_roles mr INNER JOIN roles old_role ON mr.role_id = old_role.role_id
SET mr.role_id = (SELECT role_id FROM roles WHERE name = 'CORE')
WHERE old_role.name LIKE '코어%';

UPDATE member_roles mr INNER JOIN roles old_role ON mr.role_id = old_role.role_id
SET mr.role_id = (SELECT role_id FROM roles WHERE name = 'GUEST')
WHERE old_role.name IN ('GUEST', '게스트')
  AND mr.role_id <> (SELECT role_id FROM roles WHERE name = 'GUEST');

COMMIT;

-- -----------------------------------------------------------------------------
-- [4] member_cohorts 중복 정리 + UNIQUE(member_id, cohort_id)
-- -----------------------------------------------------------------------------
START TRANSACTION;

DELETE mc1 FROM member_cohorts mc1
INNER JOIN member_cohorts mc2
    ON mc1.member_id = mc2.member_id
   AND mc1.cohort_id = mc2.cohort_id
   AND mc1.member_cohort_id > mc2.member_cohort_id;

SET @has_uk := (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema=DATABASE() AND table_name='member_cohorts' AND index_name='uk_member_cohort');
SET @sql := IF(@has_uk=0,
    'ALTER TABLE member_cohorts ADD UNIQUE KEY uk_member_cohort (member_id, cohort_id)',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

COMMIT;

-- -----------------------------------------------------------------------------
-- [5] 구시스템 아카이빙 + DROP: member_authorities / authorities
-- -----------------------------------------------------------------------------
START TRANSACTION;

CREATE TABLE IF NOT EXISTS _archive_member_authorities_20260913 AS SELECT * FROM member_authorities;
CREATE TABLE IF NOT EXISTS _archive_authorities_20260913        AS SELECT * FROM authorities;

DROP TABLE IF EXISTS member_authorities;
DROP TABLE IF EXISTS authorities;

COMMIT;

-- =============================================================================
-- VERIFY (읽기 전용) — 문서 §7.5
-- =============================================================================
-- active cohort (기대: 정확히 1개, 최신 기수)
SELECT cohort_id, `value`, is_active, activated_at FROM cohorts WHERE is_active = TRUE;

-- 특수 슬롯 (기대: cohort_id=0 존재, is_active=FALSE)
SELECT cohort_id, `value`, is_active FROM cohorts WHERE cohort_id = 0;

-- role_permissions 개수 (기대: MASTER=23, CORE=23, ORGANIZER=17, DEEPER=7, GUEST=0)
SELECT r.name, COUNT(rp.permission_id) AS perm_count
FROM roles r
LEFT JOIN role_permissions rp ON r.role_id = rp.role_id AND rp.revoked_at IS NULL
WHERE r.name IN ('MASTER', 'CORE', 'ORGANIZER', 'DEEPER', 'GUEST')
GROUP BY r.name;

-- member_roles (기대: canonical role 만 남음)
SELECT r.name, COUNT(*) AS member_count
FROM member_roles mr
JOIN roles r ON mr.role_id = r.role_id
WHERE mr.deleted_at IS NULL
GROUP BY r.name;

-- 구시스템 제거 (기대: member_authorities/authorities 부재, _archive_* 존재)
SHOW TABLES LIKE 'member_authorities';
SHOW TABLES LIKE 'authorities';
SHOW TABLES LIKE '_archive_member_authorities_%';

-- member_cohorts 유니크 (기대: 중복 0건, uk_member_cohort 인덱스 존재)
SELECT member_id, cohort_id, COUNT(*) AS dup
FROM member_cohorts
GROUP BY member_id, cohort_id
HAVING dup > 1;
SHOW INDEX FROM member_cohorts WHERE Key_name = 'uk_member_cohort';

-- =============================================================================
-- ROLLBACK (역순 실행)
-- =============================================================================
--  START TRANSACTION;
--
--  -- [5] 구시스템 복원
--  CREATE TABLE IF NOT EXISTS authorities        AS SELECT * FROM _archive_authorities_20260913;
--  CREATE TABLE IF NOT EXISTS member_authorities AS SELECT * FROM _archive_member_authorities_20260913;
--  -- 필요 시 원본 인덱스/PK/FK 재적용 (아카이브 테이블에는 제약이 포함되지 않음)
--
--  -- [4] member_cohorts UNIQUE 해제
--  ALTER TABLE member_cohorts DROP INDEX uk_member_cohort;
--
--  -- [3] member_roles 재매핑 되돌리기 (백업 스크립트가 채워둔 legacy_role_id 사용)
--  UPDATE member_roles SET role_id = legacy_role_id WHERE legacy_role_id IS NOT NULL;
--
--  -- [2] role_permissions / roles 시드 원복
--  --     주의: 사전 스냅샷이 없다면 이 단계로는 원본 매트릭스를 재구성할 수 없음.
--  --     운영 환경에서는 [3] 원복 완료 후 실행할 것 (기수 문자열 role 참조 무결성 이유).
--  DELETE rp FROM role_permissions rp
--  INNER JOIN roles r ON rp.role_id = r.role_id
--  WHERE r.name IN ('MASTER','CORE','ORGANIZER','DEEPER','GUEST');
--  DELETE FROM roles WHERE name = 'MASTER';
--
--  -- [1] cohorts 원복
--  DELETE FROM cohorts WHERE cohort_id = 0;
--  UPDATE cohorts SET is_active = FALSE, activated_at = NULL WHERE is_active = TRUE;
--  ALTER TABLE cohorts DROP COLUMN activated_at;
--  ALTER TABLE cohorts DROP COLUMN is_active;
--
--  COMMIT;
-- =============================================================================
