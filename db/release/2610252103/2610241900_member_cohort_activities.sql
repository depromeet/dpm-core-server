-- 2026-10-24 / 기수별 활동 상태 및 이력
-- 목적: 활동 정지와 기수 종료를 구분하고 동일 기수 복구를 차단한다.
-- 선행: MySQL 8, members/member_cohorts/cohorts. 가입/회원/기수 쓰기를 중지하고 백업한다.
-- 후행: VERIFY 확인 후 앱을 배포한다. 신규 앱의 ddl-auto=validate 전에 적용한다.
-- 검증: 활성 기수 1건, 연결된 승인 회원별 상태 1건, 기존 INACTIVE는 ENDED, 재실행 시 기존 행 불변.
-- 주의: DDL은 암묵적으로 COMMIT된다. DDL 롤백은 보장하지 않는다. 운영·개발 DB에는 별도 적용한다.
--       활동 기수를 알 수 없는 회원에게 임의 기수를 부여하지 않는다. 하단 unmapped_approved_members를 확인한다.
--       IF NOT EXISTS는 잘못된 기존 구조를 수정하지 않는다. 구조가 다르면 중단한다.

-- [1] 사전 검증: 기수 전환 중이거나 잘못된 소속 참조가 있으면 중단한다.
DELIMITER $$
DROP PROCEDURE IF EXISTS verify_member_activity_migration$$
CREATE PROCEDURE verify_member_activity_migration()
BEGIN
    IF (SELECT COUNT(*) FROM cohorts WHERE is_active = TRUE) <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exactly one active cohort is required';
    END IF;
    IF EXISTS (
        SELECT 1 FROM member_cohorts mc
        LEFT JOIN members m ON m.member_id = mc.member_id
        LEFT JOIN cohorts c ON c.cohort_id = mc.cohort_id
        WHERE m.member_id IS NULL OR c.cohort_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Resolve orphaned member_cohorts before migration';
    END IF;
END$$
CALL verify_member_activity_migration()$$
DROP PROCEDURE verify_member_activity_migration$$
DELIMITER ;

-- [2] 신규 테이블 (DDL)
CREATE TABLE IF NOT EXISTS member_cohort_activities (
    member_cohort_activity_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    cohort_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    suspended_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_cohort_activity_id),
    UNIQUE KEY uk_member_cohort_activity (member_id, cohort_id),
    KEY idx_cohort_activity (cohort_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS member_activity_events (
    member_activity_event_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    cohort_id BIGINT NOT NULL,
    actor_member_id BIGINT NULL,
    previous_status VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_activity_event_id),
    KEY idx_member_activity_events (member_id, cohort_id, member_activity_event_id)
) ENGINE=InnoDB;

-- [3] 기존 승인 회원 소속만 분류한다. 신규 테이블에 이미 있는 상태·정지 이력은 덮어쓰지 않는다.
START TRANSACTION;
INSERT INTO member_cohort_activities (member_id, cohort_id, status, suspended_at, created_at, updated_at)
SELECT DISTINCT m.member_id, c.cohort_id,
    CASE WHEN m.status = 'ACTIVE' AND c.is_active = TRUE THEN 'ACTIVE' ELSE 'ENDED' END,
    NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM members m
JOIN member_cohorts mc ON mc.member_id = m.member_id
JOIN cohorts c ON c.cohort_id = mc.cohort_id
WHERE m.deleted_at IS NULL AND m.status IN ('ACTIVE', 'INACTIVE')
  AND NOT EXISTS (SELECT 1 FROM member_cohort_activities a WHERE a.member_id = m.member_id AND a.cohort_id = c.cohort_id);
COMMIT;

-- [4] VERIFY (읽기 전용)
SHOW CREATE TABLE member_cohort_activities;
SHOW CREATE TABLE member_activity_events;
SELECT status, COUNT(*) AS activity_count FROM member_cohort_activities GROUP BY status;
SELECT COUNT(*) AS missing_mapped_activities
FROM (SELECT DISTINCT m.member_id, mc.cohort_id FROM members m JOIN member_cohorts mc ON mc.member_id = m.member_id
      WHERE m.deleted_at IS NULL AND m.status IN ('ACTIVE', 'INACTIVE')) expected
LEFT JOIN member_cohort_activities a ON a.member_id = expected.member_id AND a.cohort_id = expected.cohort_id
WHERE a.member_id IS NULL;
-- 0건이어야 한다. 단 아래는 소속 없는 MASTER 등일 수 있으며 임의로 배정하지 않는다.
SELECT COUNT(*) AS unmapped_approved_members FROM members m
WHERE m.deleted_at IS NULL AND m.status IN ('ACTIVE', 'INACTIVE')
  AND NOT EXISTS (SELECT 1 FROM member_cohorts mc WHERE mc.member_id = m.member_id);
SELECT COUNT(*) AS invalid_activity_status FROM member_cohort_activities WHERE status NOT IN ('ACTIVE', 'SUSPENDED', 'ENDED');
