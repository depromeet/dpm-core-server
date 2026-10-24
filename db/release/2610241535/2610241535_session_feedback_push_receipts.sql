-- =============================================================================
-- 2026-10-24 · 세션 피드백 푸시 멤버별 발송 이력
-- =============================================================================
-- 목적: 수집 기간(start_at ~ end_at) 동안 출석·지각자 중 아직 푸시를 받지 않은
--       멤버에게만 SESSION_FEEDBACK_OPENED 를 보내기 위한 발송 이력 저장
-- 선행: session_feedback_forms
-- 주의: 애플리케이션이 ddl-auto: validate 이므로 이 스크립트를 먼저 적용한 뒤 배포한다.

START TRANSACTION;

CREATE TABLE IF NOT EXISTS session_feedback_push_receipts (
    session_feedback_push_receipt_id BIGINT      NOT NULL AUTO_INCREMENT,
    session_id                       BIGINT      NOT NULL,
    member_id                        BIGINT      NOT NULL,
    sent_at                          DATETIME(6) NOT NULL,
    PRIMARY KEY (session_feedback_push_receipt_id),
    CONSTRAINT uk_session_feedback_push_receipts_session_member UNIQUE (session_id, member_id),
    INDEX idx_session_feedback_push_receipts_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='세션 피드백 수집 시작 푸시 멤버별 발송 이력';

COMMIT;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
SHOW TABLES LIKE 'session_feedback_push_receipts';
SHOW CREATE TABLE session_feedback_push_receipts\G
