-- =============================================================================
-- 2026-10-01 · 출석 자동 결석 지원 컬럼/인덱스 (#573)
-- =============================================================================
-- 대상: MySQL 8
-- 목적:
--       1) attendances(session_id, member_id) 인덱스 추가
--          (세션 단위 조건부 UPDATE 가 테이블 전체를 스캔/잠금하지 않도록)
--       2) attendances.auto_absent_at 컬럼 추가 (자동 결석 출처 표지, NULL 허용)
--          기존 행은 모두 NULL 로 둔다. 과거 ABSENT 를 자동 결석으로 추정하지 않으며,
--          애플리케이션은 이 표지가 있는 ABSENT 만 인증 덮어쓰기/마감 연장 재개 대상으로 본다.
-- 선행: 없음
-- 후행: 없음
-- 검증: 파일 하단 VERIFY 섹션 (읽기 전용)
-- 주의:
--   * 애플리케이션이 ddl-auto: validate 이므로 이 스크립트를 먼저 적용한 뒤 배포해야 한다.
--     컬럼이 없으면 AttendanceEntity 스키마 검증에 실패해 서버가 기동되지 않는다.
--   * 여러 번 실행해도 안전하다. 기존 세션 시각이나 출석 기록 값은 바꾸지 않는다.
--   * auto_absent_at 은 다른 Instant 컬럼(attended_at 등)과 같은 timestamp(6) 타입이다.
--   * 기본 출석 시간(10/15/30분)은 DB 가 아니라 환경 변수로 설정한다.
--       ATTENDANCE_OPEN_MINUTES_BEFORE_START / ATTENDANCE_LATE_AFTER_START_MINUTES / ATTENDANCE_ABSENT_AFTER_START_MINUTES
--   * 배포 후 자동 결석 스케줄러(30초 주기)는 기간 하한 없이 모든 기수의 인증 마감이 지난 세션을 처리한다.
--     과거 기수 세션이라도 미인증(PENDING, attended_at/updated_at 없음) 기록은 첫 실행에서 ABSENT 로 바뀌고
--     auto_absent_at 표지가 기록된다. 출석/지각/인정 결석/운영진 변경 기록과 표지 없는 기존 ABSENT 는 바뀌지 않는다.
--     배포 전 아래 미리보기 쿼리로 대상 규모를 확인한다.

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

-- -----------------------------------------------------------------------------
-- [2] attendances.auto_absent_at (자동 결석 출처 표지). 기존 행은 NULL 그대로 둔다(값을 채우지 않음).
-- -----------------------------------------------------------------------------
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
-- 인덱스 (기대: session_id, member_id 두 행)
SHOW INDEX FROM attendances WHERE Key_name = 'idx_attendances_session_member';

-- 표지 컬럼 (기대: auto_absent_at / timestamp(6) / YES / NULL)
SHOW COLUMNS FROM attendances LIKE 'auto_absent_at';

-- 배포 직후 (기대: 0. 기존 행은 표지가 비어 있어야 한다)
SELECT COUNT(*) AS rows_with_marker_before_deploy
FROM attendances
WHERE auto_absent_at IS NOT NULL;

-- sessions.absent_start 컬럼 타입 확인 (아래 미리보기 쿼리는 timestamp 타입 기준)
SHOW COLUMNS FROM sessions LIKE 'absent_start';

-- 배포 후 첫 스케줄러 실행에서 자동 결석될 기록 미리보기 (모든 기수, 과거 세션 포함, 하한 없음)
-- (absent_start 가 timestamp 타입이면 UNIX_TIMESTAMP(col) 은 타임존과 무관하게 정확하다.
--  datetime 타입이고 UTC 로 저장돼 있다면 UNIX_TIMESTAMP(CONVERT_TZ(s.absent_start, '+00:00', @@session.time_zone)) 로 바꾼다.)
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

-- 배포 후: 자동 결석으로 바뀐 기록. 출처는 표지(auto_absent_at)로만 판단한다.
-- (ABSENT 이고 attended_at/updated_at 이 비어 있다는 이유만으로 자동 결석으로 보지 않는다.)
SELECT a.session_id, COUNT(*) AS auto_absent, MIN(a.auto_absent_at) AS first_marked, MAX(a.auto_absent_at) AS last_marked
FROM attendances a
WHERE a.deleted_at IS NULL
  AND a.status = 'ABSENT'
  AND a.auto_absent_at IS NOT NULL
GROUP BY a.session_id;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 1) 먼저 이 컬럼을 사용하지 않는 이전 버전 애플리케이션으로 되돌린다.
--    (새 버전이 떠 있는 상태에서 컬럼을 지우면 ddl-auto: validate 로 재기동이 실패한다.)
-- 2) 필요하면 자동 결석 결과만 미인증으로 되돌린다. 대상은 표지가 있고 그 뒤 인증/운영진 변경이 없는 ABSENT 뿐이다.
--    표지가 없는 기존 결석(배포 전부터 있던 결석)은 건드리지 않는다. 되돌릴지 여부는 운영 판단이 필요하다.
--    반드시 컬럼 제거(3) 전에 실행한다.
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
-- 3) 표지 컬럼 제거 (이전 버전은 이 컬럼을 모르므로 남겨도 동작에는 문제 없다. ddl-auto: validate 는 추가 컬럼을 허용한다)
--
--  ALTER TABLE attendances DROP COLUMN auto_absent_at;
--
-- 4) 인덱스는 이전 버전에도 무해하므로 남겨도 된다. 제거하려면:
--
--  ALTER TABLE attendances DROP INDEX idx_attendances_session_member;
-- =============================================================================
