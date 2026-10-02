-- =============================================================================
-- 2026-10-01 · attendances(session_id, member_id) 인덱스 (#573)
-- =============================================================================
-- 대상: MySQL 8
-- 목적: 세션 단위 조건부 UPDATE(운영진 변경, 세션 삭제, 시각 변경 재판정)가 테이블 전체를 잠그지 않도록 한다.
-- 선행: 없음 / 후행: 2610011200_attendance_auto_absence.sql
-- 주의: 출석 인증 마감/동시 쓰기 정합성 변경 배포 전에 적용한다. 여러 번 실행해도 안전하며 온라인 DDL 이다.

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
-- 기대: session_id, member_id 두 행
SHOW INDEX FROM attendances WHERE Key_name = 'idx_attendances_session_member';

-- 기대: key = idx_attendances_session_member (실행하지 않고 계획만 본다)
EXPLAIN UPDATE attendances SET updated_at = updated_at WHERE session_id = 0 AND deleted_at IS NULL;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 이전 버전에도 무해하므로 남겨 두는 것을 권장한다. 자동 결석 배포 후에는 제거하지 않는다.
--
--  ALTER TABLE attendances DROP INDEX idx_attendances_session_member;
-- =============================================================================
