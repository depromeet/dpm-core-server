-- 반려 전에 완료한 프로필의 입력 완료 표시만 해제한다. 이름/파트와 이력은 보존한다.
-- 선행: member_admission_events, members.profile_completed_at. 백업 후 회원 쓰기를 중지하고 적용한다.
-- UTC로 저장한 완료 시각과 반려 시각을 비교한다. 반려 후 재입력한 회원은 재실행해도 보존한다.
-- 반려 이력이 없는 REJECTED는 자동 수정하지 않으며 아래 VERIFY 결과를 별도 확인한다.

START TRANSACTION;
UPDATE members m
JOIN (
    SELECT member_id, MAX(created_at) AS rejected_at
    FROM member_admission_events
    WHERE event_type = 'REJECTED'
    GROUP BY member_id
) e ON e.member_id = m.member_id
SET m.profile_completed_at = NULL
WHERE m.status = 'REJECTED'
  AND m.deleted_at IS NULL
  AND m.profile_completed_at <= e.rejected_at;
COMMIT;

-- VERIFY: remaining_reset_targets는 0이어야 한다.
SELECT COUNT(*) AS remaining_reset_targets
FROM members m
JOIN (
    SELECT member_id, MAX(created_at) AS rejected_at
    FROM member_admission_events
    WHERE event_type = 'REJECTED'
    GROUP BY member_id
) e ON e.member_id = m.member_id
WHERE m.status = 'REJECTED'
  AND m.deleted_at IS NULL
  AND m.profile_completed_at <= e.rejected_at;

SELECT COUNT(*) AS rejected_members_without_rejection_event
FROM members m
WHERE m.status = 'REJECTED'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM member_admission_events e
      WHERE e.member_id = m.member_id AND e.event_type = 'REJECTED'
  );
