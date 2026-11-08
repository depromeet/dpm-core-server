-- 2026-10-30 / 가입 정보 미제출 회원의 미승인 NEW 배지 정리
-- 목적: 이름·파트를 제출하지 않은 PENDING 회원을 저장된 미승인 배지 대상에서 제외한다.
-- 선행: members.profile_completed_at, member_badge_states, member_badge_memberships. DB 백업 후 회원 쓰기를 중지한다.
-- 후행: VERIFY 확인 후 제출 완료 기준 앱을 배포하고 쓰기를 재개한다. 구 버전이 쓰면 제외한 대상이 다시 추가될 수 있다.
-- 검증: 대상 0건, 영향 기수의 target_count 일치, 빈 대상의 acknowledged_version = version. 재실행 결과 동일.
-- 주의: 회원·소셜 연결·기수와 다른 배지는 변경하지 않는다. 유효한 대상이 남으면 기존 version/acknowledged_version을 유지한다.
--       과거 NEW 증가분의 원인을 회원별로 복원할 수 없으므로 남은 대상의 미확인 NEW를 일괄 초기화하지 않는다.
--       기존 프로필 완료 backfill 값은 보존한다. 본 SQL은 완료 표시를 만들거나 지우지 않는다.

-- [1] 이번 정리로 영향을 받는 기수만 고정한다. 임시 테이블은 현재 연결에서만 사용한다.
DROP TEMPORARY TABLE IF EXISTS pending_submission_badge_cohorts;
CREATE TEMPORARY TABLE pending_submission_badge_cohorts (cohort_id BIGINT PRIMARY KEY);
START TRANSACTION;
INSERT INTO pending_submission_badge_cohorts (cohort_id)
SELECT DISTINCT b.cohort_id
FROM member_badge_memberships b
JOIN members m ON m.member_id = b.member_id
WHERE b.card = 'PENDING' AND m.status = 'PENDING' AND m.profile_completed_at IS NULL;

-- 기존 앱의 배지 잠금 순서(상태 -> 대상)를 유지한다.
SELECT s.cohort_id, s.card
FROM member_badge_states s
JOIN pending_submission_badge_cohorts affected ON affected.cohort_id = s.cohort_id
WHERE s.card = 'PENDING'
ORDER BY s.cohort_id, s.card
FOR UPDATE;

-- [2] 미제출 회원의 배지 연결만 제거하고 해당 기수의 대상 수를 맞춘다.
DELETE b FROM member_badge_memberships b
JOIN members m ON m.member_id = b.member_id
JOIN pending_submission_badge_cohorts affected ON affected.cohort_id = b.cohort_id
WHERE b.card = 'PENDING' AND m.status = 'PENDING' AND m.profile_completed_at IS NULL;

UPDATE member_badge_states s
JOIN pending_submission_badge_cohorts affected ON affected.cohort_id = s.cohort_id
SET s.target_count = (SELECT COUNT(*) FROM member_badge_memberships b WHERE b.cohort_id = s.cohort_id AND b.card = 'PENDING'),
    s.acknowledged_version = CASE
        WHEN NOT EXISTS (SELECT 1 FROM member_badge_memberships b WHERE b.cohort_id = s.cohort_id AND b.card = 'PENDING') THEN s.version
        ELSE s.acknowledged_version
    END
WHERE s.card = 'PENDING';
COMMIT;

-- [3] VERIFY (모두 0건이어야 한다)
SELECT COUNT(*) AS unfinished_pending_badge_memberships
FROM member_badge_memberships b
JOIN members m ON m.member_id = b.member_id
WHERE b.card = 'PENDING' AND m.status = 'PENDING' AND m.profile_completed_at IS NULL;
SELECT COUNT(*) AS inconsistent_pending_badge_states
FROM member_badge_states s
JOIN pending_submission_badge_cohorts affected ON affected.cohort_id = s.cohort_id
WHERE s.card = 'PENDING' AND (
    s.target_count <> (SELECT COUNT(*) FROM member_badge_memberships b WHERE b.cohort_id = s.cohort_id AND b.card = 'PENDING')
    OR (s.target_count = 0 AND s.acknowledged_version <> s.version)
);
DROP TEMPORARY TABLE pending_submission_badge_cohorts;
