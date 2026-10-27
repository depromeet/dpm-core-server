-- 2026-10-27 / 운영진 멤버 삭제 권한
-- 목적: ORGANIZER에 기존 MEMBER/DELETE 권한 하나를 부여한다.
-- 선행: roles, permissions, role_permissions 및 ORGANIZER, MEMBER/DELETE 시드가 존재해야 한다.
-- 후행: VERIFY에서 활성 권한 1건을 확인한다. 기존 MASTER 전용 purge 검사는 유지한다.
-- 검증: 재실행 후에도 권한 행이 중복 생성되지 않으며 다른 역할·권한은 변경하지 않는다.
-- 주의: create:member도 가진 운영진은 기존 병합 API의 권한 조건을 충족한다.
--       아래 역할·권한 조회가 각각 1이 아니거나 활성 연결이 2건 이상이면 적용하지 않는다.

-- [1] 사전 확인 (읽기 전용)
SELECT COUNT(*) AS organizer_role_count FROM roles WHERE name = 'ORGANIZER';
SELECT COUNT(*) AS delete_permission_count FROM permissions WHERE resource = 'MEMBER' AND action = 'DELETE';
SELECT COUNT(*) AS active_permission_count
FROM role_permissions rp
JOIN roles r ON r.role_id = rp.role_id
JOIN permissions p ON p.permission_id = rp.permission_id
WHERE r.name = 'ORGANIZER' AND p.resource = 'MEMBER' AND p.action = 'DELETE' AND rp.revoked_at IS NULL;

-- [2] 누락된 연결만 추가하거나 기존 취소된 연결 하나를 복구
START TRANSACTION;
SET @organizer_role_id = (SELECT role_id FROM roles WHERE name = 'ORGANIZER');
SET @member_delete_permission_id = (SELECT permission_id FROM permissions WHERE resource = 'MEMBER' AND action = 'DELETE');
SET @organizer_delete_link_id = (
    SELECT role_permission_id FROM role_permissions
    WHERE role_id = @organizer_role_id AND permission_id = @member_delete_permission_id
    ORDER BY (revoked_at IS NULL) DESC, role_permission_id ASC LIMIT 1
);
UPDATE role_permissions
SET revoked_at = NULL, granted_at = UTC_TIMESTAMP(6)
WHERE role_permission_id = @organizer_delete_link_id AND revoked_at IS NOT NULL;
INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT @organizer_role_id, @member_delete_permission_id, UTC_TIMESTAMP(6)
WHERE @organizer_role_id IS NOT NULL AND @member_delete_permission_id IS NOT NULL
  AND @organizer_delete_link_id IS NULL;
COMMIT;

-- [3] VERIFY (읽기 전용): 활성 연결 1건
SELECT r.name, p.resource, p.action, rp.revoked_at
FROM role_permissions rp
JOIN roles r ON r.role_id = rp.role_id
JOIN permissions p ON p.permission_id = rp.permission_id
WHERE r.name = 'ORGANIZER' AND p.resource = 'MEMBER' AND p.action = 'DELETE' AND rp.revoked_at IS NULL;
