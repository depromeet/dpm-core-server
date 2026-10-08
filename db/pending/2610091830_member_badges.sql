-- 2026-10-09 / 멤버 관리 카드 NEW 상태
-- 목적: 현재 기수 카드의 신규 진입 버전, 운영진 공통 확인 버전과 이전 대상을 저장한다.
-- 선행: members, cohorts 및 멤버 관리 조회 기능. 이 SQL을 애플리케이션 배포 전에 적용한다.
-- 후행: 애플리케이션의 공통 조회로 최초 접근/첫 쓰기 직전 대상을 확인 완료 상태로 초기화한다.
-- 검증: 하단 VERIFY, 로컬 MySQL 전이 및 동시성 테스트. 운영 데이터에 테스트를 실행하지 않는다.
-- 주의: DDL은 MySQL에서 암묵적 COMMIT된다. 기존 테이블/데이터를 변경하지 않으며 재실행해도 상태를 초기화하지 않는다.

-- [1] 저장 테이블
START TRANSACTION;
CREATE TABLE IF NOT EXISTS member_badge_states (
    cohort_id BIGINT NOT NULL,
    card VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    acknowledged_version BIGINT NOT NULL DEFAULT 0,
    target_count INT NOT NULL DEFAULT 0,
    initialized BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (cohort_id, card)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS member_badge_memberships (
    cohort_id BIGINT NOT NULL,
    card VARCHAR(20) NOT NULL,
    member_id BIGINT NOT NULL,
    PRIMARY KEY (cohort_id, card, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
COMMIT;

-- [2] VERIFY (읽기 전용)
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('member_badge_states', 'member_badge_memberships')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
SELECT cohort_id, card, version, acknowledged_version, target_count, initialized
FROM member_badge_states WHERE acknowledged_version > version OR target_count < 0;
SELECT s.cohort_id, s.card, s.target_count, COUNT(m.member_id) AS actual_count
FROM member_badge_states s LEFT JOIN member_badge_memberships m ON m.cohort_id = s.cohort_id AND m.card = s.card
GROUP BY s.cohort_id, s.card, s.target_count HAVING s.target_count <> COUNT(m.member_id);
