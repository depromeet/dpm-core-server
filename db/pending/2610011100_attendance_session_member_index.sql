-- =============================================================================
-- 2026-10-01 · attendances(session_id, member_id) 인덱스 (#573)
-- =============================================================================
-- 대상: MySQL 8
-- 목적: attendances(session_id, member_id) 인덱스 추가
--       세션 단위 조건부 UPDATE(운영진 출석 변경, 세션 삭제 동기화, 세션 시각 변경 재판정)가
--       테이블 전체를 스캔/잠금하지 않도록 한다. 인덱스가 없으면 이 UPDATE 들이 모든 출석 행을 잠가
--       다른 세션의 출석 인증까지 대기시킨다.
-- 선행: 없음
-- 후행: 2610011200_attendance_auto_absence.sql
-- 검증: 파일 하단 VERIFY 섹션 (읽기 전용)
-- 주의:
--   * 출석 인증 마감/동시 쓰기 정합성 변경을 배포하기 전에 적용한다.
--     ddl-auto: validate 는 인덱스를 검사하지 않으므로 없어도 기동은 되지만 잠금 범위가 테이블 전체로 넓어진다.
--   * 여러 번 실행해도 안전하다. 데이터 값은 바꾸지 않는다.
--   * MySQL 8 의 보조 인덱스 추가는 온라인 DDL 로 처리되어 읽기/쓰기를 막지 않는다.

-- -----------------------------------------------------------------------------
-- [1] attendances(session_id, member_id) 인덱스
-- -----------------------------------------------------------------------------
SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE()
                   AND table_name = 'attendances'
                   AND index_name = 'idx_attendances_session_member');
SET @sql_idx := IF(@has_idx = 0,
    'ALTER TABLE attendances ADD INDEX idx_attendances_session_member (session_id, member_id)',
    'SELECT 1');
PREPARE s_idx FROM @sql_idx; EXECUTE s_idx; DEALLOCATE PREPARE s_idx;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
-- 인덱스 (기대: session_id, member_id 두 행)
SHOW INDEX FROM attendances WHERE Key_name = 'idx_attendances_session_member';

-- 세션 단위 UPDATE 가 인덱스를 쓰는지 실행 계획만 확인한다(실행하지 않음).
-- (기대: key = idx_attendances_session_member, type 이 ALL 이 아님)
EXPLAIN UPDATE attendances SET updated_at = updated_at WHERE session_id = 0 AND deleted_at IS NULL;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 인덱스는 이전 버전 애플리케이션에도 무해하므로 남겨 두는 것을 권장한다.
-- 2610011200_attendance_auto_absence.sql 이후 버전(자동 결석)은 이 인덱스 순서로 행 잠금을 잡으므로
-- 그 버전이 배포된 상태에서는 제거하지 않는다. 제거하려면:
--
--  ALTER TABLE attendances DROP INDEX idx_attendances_session_member;
-- =============================================================================
