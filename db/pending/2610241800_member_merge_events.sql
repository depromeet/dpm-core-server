-- 2026-10-24 / 회원 병합 처리 이력
-- 목적: 병합 처리자와 남긴 회원, 정리한 회원, 처리 시점을 기록한다.
-- 선행: members 및 기존 소셜 계정 연동 SQL 적용 완료.
-- 후행: 이 SQL의 VERIFY 확인 후 병합 기능 앱을 배포한다.
-- 검증: 컬럼 타입·NULL 제약·인덱스·InnoDB 엔진이 엔티티와 일치해야 한다.
-- 주의: 이름·이메일·토큰을 저장하지 않는다. 회원 삭제 후에도 식별자 이력은 보존한다.
--       MySQL DDL은 암묵적으로 COMMIT되므로 트랜잭션으로 DDL 롤백을 보장하지 않는다.
--       IF NOT EXISTS는 기존 구조를 보정하지 않으므로 재실행 시에도 VERIFY가 필요하다.

-- [1] 이력 테이블 생성 (DDL)
CREATE TABLE IF NOT EXISTS member_merge_events (
    member_merge_event_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_member_id BIGINT NOT NULL,
    retained_member_id BIGINT NOT NULL,
    source_member_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_merge_event_id),
    INDEX idx_member_merge_events_retained (retained_member_id)
) ENGINE=InnoDB;

-- [2] VERIFY (읽기 전용)
SHOW CREATE TABLE member_merge_events;
SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'member_merge_events'
ORDER BY ordinal_position;
SELECT index_name, column_name, non_unique
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'member_merge_events';
