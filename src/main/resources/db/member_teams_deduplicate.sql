-- member_teams 중복 정리 스크립트
-- 같은 member_id에 대해 가장 최신 member_team_id 1건만 남기고 삭제합니다.

DELETE mt
FROM member_teams mt
JOIN member_teams newer
    ON newer.member_id = mt.member_id
    AND newer.member_team_id > mt.member_team_id;

-- 중복 정리 후, 멤버당 팀 1건만 허용하려면 아래 유니크 인덱스를 추가하세요.
-- ALTER TABLE member_teams
--     ADD CONSTRAINT uq_member_teams_member_id UNIQUE (member_id);
