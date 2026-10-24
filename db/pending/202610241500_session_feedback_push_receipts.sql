-- =============================================================================
-- 2026-10-24 · 세션 피드백 푸시 멤버별 발송 이력
-- =============================================================================
-- 목적: 수집 기간(start_at ~ end_at) 동안 출석·지각자 중 아직 푸시를 받지 않은
--       멤버에게만 SESSION_FEEDBACK_OPENED 를 보내기 위한 발송 이력 저장
-- 선행: session_feedback_forms
-- 주의: 애플리케이션이 ddl-auto: validate 이므로 이 스크립트를 먼저 적용한 뒤 배포한다.
--       MySQL DDL은 암묵적으로 커밋된다. DBeaver 등은 스크립트 전체 실행(Execute SQL Script)을 쓴다.
--       VERIFY의 \G 는 mysql CLI 전용이라 JDBC 클라이언트에서는 쓰지 않는다.

-- [1] session_feedback_push_receipts
START TRANSACTION;
CREATE TABLE IF NOT EXISTS session_feedback_push_receipts (
    session_feedback_push_receipt_id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    sent_at DATETIME(6) NOT NULL,
    PRIMARY KEY (session_feedback_push_receipt_id),
    UNIQUE KEY uk_session_feedback_push_receipts_session_member (session_id, member_id),
    KEY idx_session_feedback_push_receipts_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='세션 피드백 수집 시작 푸시 멤버별 발송 이력';
COMMIT;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
SHOW TABLES LIKE 'session_feedback_push_receipts';
SHOW CREATE TABLE session_feedback_push_receipts;
