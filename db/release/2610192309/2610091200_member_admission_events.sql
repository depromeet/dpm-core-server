-- 2026-10-09 가입 반려 및 재신청 이력
-- 목적: REJECTED 상태와 명시적 재신청의 이력을 상태 전환 후에도 보존한다.
-- 선행: members 테이블. members.status는 문자열 컬럼이며 REJECTED 값을 저장할 수 있어야 한다.
-- 후행: 이 SQL 적용 및 VERIFY 확인 후 가입 반려/재신청 앱을 배포한다.
-- 검증: 하단 VERIFY의 컬럼, 인덱스, 엔진을 엔티티와 비교한다.
-- 주의: 개인정보/반려 사유를 저장하지 않는다. 회원 소프트 삭제 시에도 이력은 보존한다.
--       MySQL DDL은 암묵적으로 커밋되므로 START TRANSACTION으로 DDL 롤백을 보장하지 않는다.
--       IF NOT EXISTS는 재실행 시 기존 구조를 수정하지 않으므로 VERIFY로 구조 차이를 확인한다.

-- [1] 이력 테이블 생성 (DDL)
START TRANSACTION;
CREATE TABLE IF NOT EXISTS member_admission_events (
    member_admission_event_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_admission_event_id),
    INDEX idx_member_admission_events_member (member_id)
) ENGINE=InnoDB;
COMMIT;

-- [2] VERIFY (읽기 전용)
SHOW CREATE TABLE member_admission_events;
SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'member_admission_events'
ORDER BY ordinal_position;
SELECT index_name, column_name, non_unique
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'member_admission_events';
SELECT column_type FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'members' AND column_name = 'status';
