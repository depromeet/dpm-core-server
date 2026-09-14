-- =============================================================================
-- 2026-09-13 · Phase 2 본작업: 스키마 정합화
-- =============================================================================
-- 목적: 문서 §Phase 2 반영. 아래 4개 원본 스크립트를 하나로 합침.
--       1) member_roles.cohort_id 컬럼 + 인덱스 idx_member_role_cohort
--       2) legacy_role_name 에서 cohort_id 채우기 (ORGANIZER / DEEPER 만)
--       3) canonical role (MASTER/CORE/ORGANIZER/DEEPER/GUEST) 외 아카이빙 + 삭제
--       4) member_cohorts.cohort_value 컬럼 제거
-- 선행: 20260913_role_system_seed.sql, 20260913_backup_member_roles.sql
-- 후행: 없음 (Phase 2 최종)
-- 검증: 파일 하단 VERIFY 섹션 (읽기 전용)
-- 주의: 스키마 삭제 포함 — 롤백 비용 큼. 실행 전 논리 백업 필수.

-- -----------------------------------------------------------------------------
-- [1] member_roles.cohort_id 컬럼 + 인덱스
-- -----------------------------------------------------------------------------
START TRANSACTION;

SET @has_cohort_id := (SELECT COUNT(*) FROM information_schema.columns
                       WHERE table_schema=DATABASE() AND table_name='member_roles' AND column_name='cohort_id');
SET @sql1 := IF(@has_cohort_id=0,
    'ALTER TABLE member_roles ADD COLUMN cohort_id BIGINT NULL',
    'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema=DATABASE() AND table_name='member_roles' AND index_name='idx_member_role_cohort');
SET @sql2 := IF(@has_idx=0,
    'ALTER TABLE member_roles ADD INDEX idx_member_role_cohort (member_id, cohort_id)',
    'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

COMMIT;

-- -----------------------------------------------------------------------------
-- [2] legacy_role_name 에서 cohort_id 채우기
--     ORGANIZER / DEEPER 만 대상. MASTER / CORE / GUEST 는 NULL 유지
--     (애플리케이션 레이어에서 cohort_id=0 특수 슬롯으로 보정)
-- -----------------------------------------------------------------------------
START TRANSACTION;

UPDATE member_roles mr
INNER JOIN cohorts c ON c.`value` = REGEXP_SUBSTR(mr.legacy_role_name, '^[0-9]+')
INNER JOIN roles   r ON mr.role_id = r.role_id
SET mr.cohort_id = c.cohort_id
WHERE mr.legacy_role_name REGEXP '^[0-9]+기'
  AND r.name IN ('ORGANIZER', 'DEEPER');

COMMIT;

-- -----------------------------------------------------------------------------
-- [3] 구 roles 아카이빙 + canonical 이외 삭제
-- -----------------------------------------------------------------------------
START TRANSACTION;

CREATE TABLE IF NOT EXISTS _archive_roles_20260913 AS SELECT * FROM roles;

DELETE rp FROM role_permissions rp
INNER JOIN roles r ON rp.role_id = r.role_id
WHERE r.name NOT IN ('MASTER', 'CORE', 'ORGANIZER', 'DEEPER', 'GUEST');

DELETE FROM roles WHERE name NOT IN ('MASTER', 'CORE', 'ORGANIZER', 'DEEPER', 'GUEST');

COMMIT;

-- -----------------------------------------------------------------------------
-- [4] member_cohorts.cohort_value 컬럼 제거 (레거시)
-- -----------------------------------------------------------------------------
START TRANSACTION;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema=DATABASE() AND table_name='member_cohorts' AND column_name='cohort_value');
SET @sql := IF(@has_col>0,
    'ALTER TABLE member_cohorts DROP COLUMN cohort_value',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

COMMIT;

-- =============================================================================
-- VERIFY (읽기 전용) — 문서 §7.5
-- =============================================================================
-- cohort_id 채움 (기대: ORGANIZER/DEEPER 는 null_cohort=0, MASTER/CORE/GUEST 는 NULL 다수)
SELECT r.name,
       COUNT(*)                       AS total,
       COUNT(mr.cohort_id)            AS with_cohort,
       COUNT(*) - COUNT(mr.cohort_id) AS null_cohort
FROM member_roles mr
JOIN roles r ON mr.role_id = r.role_id
WHERE mr.deleted_at IS NULL
GROUP BY r.name;

-- canonical roles 5개만 (기대: MASTER/CORE/ORGANIZER/DEEPER/GUEST)
SELECT COUNT(*) AS role_count, GROUP_CONCAT(name ORDER BY name) AS names FROM roles;

-- cohort_value 제거 (기대: 빈 결과)
SHOW COLUMNS FROM member_cohorts LIKE 'cohort_value';

-- =============================================================================
-- ROLLBACK (역순 실행) — 스키마 삭제 포함이라 아카이브 의존
-- =============================================================================
--  START TRANSACTION;
--
--  -- [4] cohort_value 컬럼 재추가 (값 복원은 별도 백업이 있어야 가능)
--  ALTER TABLE member_cohorts ADD COLUMN cohort_value VARCHAR(255) NULL;
--  -- 필요 시: UPDATE member_cohorts mc JOIN cohorts c ON mc.cohort_id = c.cohort_id
--  --         SET mc.cohort_value = c.`value`;
--
--  -- [3] 기수 문자열 role 복원 (아카이브에서 되돌리기)
--  INSERT INTO roles (role_id, name)
--  SELECT role_id, name FROM _archive_roles_20260913
--  WHERE name NOT IN ('MASTER','CORE','ORGANIZER','DEEPER','GUEST')
--  ON DUPLICATE KEY UPDATE name = VALUES(name);
--  -- role_permissions 는 별도 스냅샷이 없으면 재구성 불가.
--
--  -- [2] cohort_id 채우기 되돌리기
--  UPDATE member_roles SET cohort_id = NULL;
--
--  -- [1] 인덱스 + 컬럼 제거
--  ALTER TABLE member_roles DROP INDEX idx_member_role_cohort;
--  ALTER TABLE member_roles DROP COLUMN cohort_id;
--
--  COMMIT;
-- =============================================================================
