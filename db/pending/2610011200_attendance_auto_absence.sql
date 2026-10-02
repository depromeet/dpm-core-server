-- =============================================================================
-- 2026-10-01 · 출석 자동 결석 출처 표지 컬럼 (#573)
-- =============================================================================
-- 대상: MySQL 8
-- 목적: attendances.auto_absent_at 추가(NULL 허용). 기존 행은 NULL 로 두며 과거 ABSENT 를 자동 결석으로 추정하지 않는다.
-- 선행: 2610011100_attendance_session_member_index.sql (자동 결석 UPDATE 가 이 인덱스 순서로 행을 잠근다)
-- 후행: 없음
-- 주의:
--   * ddl-auto: validate 라 배포 전에 적용해야 한다. 여러 번 실행해도 안전하다.
--   * 배포 후 첫 스케줄러 실행에서 과거 기수를 포함해 마감이 지난 모든 미인증 기록이 ABSENT 가 된다.
--     배포 전 아래 미리보기 쿼리로 대상 규모를 확인한다.

SET @has_auto_absent_at := (SELECT COUNT(*) FROM information_schema.columns
                            WHERE table_schema = DATABASE()
                              AND table_name = 'attendances'
                              AND column_name = 'auto_absent_at');
SET @sql_col := IF(@has_auto_absent_at = 0,
    'ALTER TABLE attendances ADD COLUMN auto_absent_at TIMESTAMP(6) NULL DEFAULT NULL COMMENT ''자동 결석 처리 시각(자동 결석 출처 표지)''',
    'SELECT 1');
PREPARE s_col FROM @sql_col; EXECUTE s_col; DEALLOCATE PREPARE s_col;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
-- 선행 인덱스 (기대: session_id, member_id 두 행)
SHOW INDEX FROM attendances WHERE Key_name = 'idx_attendances_session_member';

-- 기대: auto_absent_at / timestamp(6) / YES / NULL
SHOW COLUMNS FROM attendances LIKE 'auto_absent_at';

-- 배포 직후 기대: 0
SELECT COUNT(*) AS rows_with_marker_before_deploy
FROM attendances
WHERE auto_absent_at IS NOT NULL;

-- 첫 실행에서 자동 결석될 기록 미리보기(absent_start 가 timestamp 타입 기준.
-- datetime(UTC) 이면 UNIX_TIMESTAMP(CONVERT_TZ(s.absent_start, '+00:00', @@session.time_zone)) 로 바꾼다)
SHOW COLUMNS FROM sessions LIKE 'absent_start';
SELECT s.cohort_id, s.session_id, s.week, s.absent_start, COUNT(*) AS pending_to_close
FROM sessions s
JOIN attendances a ON a.session_id = s.session_id
WHERE s.deleted_at IS NULL
  AND a.deleted_at IS NULL
  AND a.status = 'PENDING'
  AND a.attended_at IS NULL
  AND a.updated_at IS NULL
  AND UNIX_TIMESTAMP(s.absent_start) <= UNIX_TIMESTAMP()
GROUP BY s.cohort_id, s.session_id, s.week, s.absent_start
ORDER BY s.absent_start;

-- 배포 후 자동 결석 기록. 출처는 표지로만 판단한다.
SELECT a.session_id, COUNT(*) AS auto_absent, MIN(a.auto_absent_at) AS first_marked, MAX(a.auto_absent_at) AS last_marked
FROM attendances a
WHERE a.deleted_at IS NULL
  AND a.status = 'ABSENT'
  AND a.auto_absent_at IS NOT NULL
GROUP BY a.session_id;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 1) 이 컬럼을 쓰지 않는 이전 버전 애플리케이션으로 먼저 되돌린다(컬럼부터 지우면 validate 로 기동 실패).
-- 2) 필요하면(운영 판단) 컬럼 제거 전에 자동 결석 결과만 미인증으로 되돌린다. 표지 없는 기존 결석은 건드리지 않는다.
--
--  START TRANSACTION;
--  UPDATE attendances
--  SET status = 'PENDING', auto_absent_at = NULL
--  WHERE status = 'ABSENT'
--    AND auto_absent_at IS NOT NULL
--    AND attended_at IS NULL
--    AND updated_at IS NULL
--    AND deleted_at IS NULL;
--  COMMIT;
--
-- 3) 컬럼 제거(남겨도 이전 버전 동작에는 문제 없다). 인덱스는 2610011100 의 롤백을 따른다.
--
--  ALTER TABLE attendances DROP COLUMN auto_absent_at;
-- =============================================================================
