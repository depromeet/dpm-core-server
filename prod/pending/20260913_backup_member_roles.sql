-- =============================================================================
-- 2026-09-13 · Phase 1 안전장치: member_roles 레거시 백업
-- =============================================================================
-- 목적: Phase 1 재매핑(role_id 교체)/Phase 2 cohort_id 채우기 실행 전, 원본을
--       같은 테이블의 legacy_role_id / legacy_role_name 컬럼에 백업한다.
-- 선행: 없음 (Phase 1 최초 스텝)
-- 후행: 20260913_role_system_seed.sql

START TRANSACTION;

SET @has_legacy_id := (SELECT COUNT(*) FROM information_schema.columns
                       WHERE table_schema=DATABASE() AND table_name='member_roles' AND column_name='legacy_role_id');
SET @sql1 := IF(@has_legacy_id=0,
    'ALTER TABLE member_roles ADD COLUMN legacy_role_id BIGINT NULL COMMENT ''Phase1 실행 전 role_id 백업''',
    'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @has_legacy_name := (SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema=DATABASE() AND table_name='member_roles' AND column_name='legacy_role_name');
SET @sql2 := IF(@has_legacy_name=0,
    'ALTER TABLE member_roles ADD COLUMN legacy_role_name VARCHAR(255) NULL COMMENT ''Phase1 실행 전 roles.name 백업''',
    'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

UPDATE member_roles mr
INNER JOIN roles r ON mr.role_id = r.role_id
SET mr.legacy_role_id   = mr.role_id,
    mr.legacy_role_name = r.name
WHERE mr.legacy_role_id IS NULL;

COMMIT;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 이 스크립트는 컬럼 추가 + 값 복사만 수행. 롤백은 컬럼 DROP만으로 충분.
-- 단, 후속 스크립트(role_system_seed) 재매핑을 되돌리려면 legacy 값을 이용해
-- 원본 role_id 로 복원하는 UPDATE 를 먼저 실행할 것.
--
-- ROLLBACK:
--   START TRANSACTION;
--   -- (선택) 재매핑 되돌리기 — Phase 1 role_id 재매핑 실행 이후에만
--   UPDATE member_roles SET role_id = legacy_role_id WHERE legacy_role_id IS NOT NULL;
--   ALTER TABLE member_roles DROP COLUMN legacy_role_id;
--   ALTER TABLE member_roles DROP COLUMN legacy_role_name;
--   COMMIT;
-- =============================================================================
