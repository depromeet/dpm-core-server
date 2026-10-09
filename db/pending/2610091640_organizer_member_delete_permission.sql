-- =============================================================================
-- 2026-10-09 운영진 멤버 삭제 권한 추가
-- =============================================================================
-- 목적: ORGANIZER에 MEMBER DELETE를 부여하여 멤버 관리 권한을 CORE와 맞춘다.
-- 선행: 2609142320_role_system_seed.sql (roles 및 permissions 시드)
-- 후행: 없음
-- 검증: 하단 VERIFY에서 ORGANIZER MEMBER DELETE의 활성 권한 수가 1인지 확인
-- 주의: 다른 도메인 권한과 과거 회수 이력은 변경하지 않는다.
--       한 담당자가 순서대로 적용한다. 적용 이력을 확인하고 앱 병합 전에 실행한다.

-- [1] 누락된 활성 권한만 추가
START TRANSACTION;

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.role_id, p.permission_id, NOW()
FROM roles r
JOIN permissions p ON p.resource = 'MEMBER' AND p.action = 'DELETE'
WHERE r.name = 'ORGANIZER'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.role_id
        AND rp.permission_id = p.permission_id
        AND rp.revoked_at IS NULL
  );

COMMIT;

-- [2] VERIFY (읽기 전용)
-- 기대: organizer_count = 1, permission_count = 1, active_grant_count = 1
-- 0이면 선행 시드가 누락된 것이므로 앱 병합 전에 확인한다.
SELECT
    (SELECT COUNT(*) FROM roles WHERE name = 'ORGANIZER') AS organizer_count,
    (SELECT COUNT(*) FROM permissions WHERE resource = 'MEMBER' AND action = 'DELETE') AS permission_count,
    (SELECT COUNT(*)
     FROM role_permissions rp
     JOIN roles r ON r.role_id = rp.role_id
     JOIN permissions p ON p.permission_id = rp.permission_id
     WHERE r.name = 'ORGANIZER'
       AND p.resource = 'MEMBER' AND p.action = 'DELETE'
       AND rp.revoked_at IS NULL) AS active_grant_count;
