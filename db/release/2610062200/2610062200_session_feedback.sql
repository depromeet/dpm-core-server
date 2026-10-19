-- =============================================================================
-- 2026-10-06 · 세션 피드백 테이블 추가 (#595)
-- =============================================================================
-- 목적: 세션 피드백 설정·응답·선택 항목 저장
--       1) session_feedback_forms   (세션 1:1, 피드백 받기 ON 일 때만 생성)
--       2) session_feedbacks         (디퍼 응답, (session_id, member_id) UNIQUE)
--       3) session_feedback_aspects  (응답별 선택 항목, 집계용)
-- 선행: 없음
-- 후행: #596 세션 CRUD, #598 진입, #599 제출, #600 홈 카드, #601 인사이트, #602 PUSH
-- 검증: 파일 하단 VERIFY 섹션 (읽기 전용)
-- 주의: 애플리케이션이 ddl-auto: validate 이므로 이 스크립트를 먼저 적용한 뒤 배포해야 한다.
--       sessions 테이블은 건드리지 않는다 (별도 테이블로 분리).

-- -----------------------------------------------------------------------------
-- [1] session_feedback_forms
-- -----------------------------------------------------------------------------
START TRANSACTION;

CREATE TABLE IF NOT EXISTS session_feedback_forms (
    session_feedback_form_id BIGINT       NOT NULL AUTO_INCREMENT,
    session_id               BIGINT       NOT NULL,
    start_at                 DATETIME(6)  NOT NULL,
    end_at                   DATETIME(6)  NOT NULL,
    push_enabled             BIT(1)       NOT NULL,
    push_sent_at             DATETIME(6)  NULL,
    created_at               DATETIME(6)  NULL,
    updated_at               DATETIME(6)  NULL,
    deleted_at               DATETIME(6)  NULL,
    PRIMARY KEY (session_feedback_form_id),
    CONSTRAINT uk_session_feedback_forms_session_id UNIQUE (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='세션 피드백 설문 설정 (세션 1:1, ON 일 때만 생성)';

COMMIT;

-- -----------------------------------------------------------------------------
-- [2] session_feedbacks
-- -----------------------------------------------------------------------------
START TRANSACTION;

CREATE TABLE IF NOT EXISTS session_feedbacks (
    session_feedback_id BIGINT        NOT NULL AUTO_INCREMENT,
    session_id          BIGINT        NOT NULL,
    member_id           BIGINT        NOT NULL,
    satisfaction        VARCHAR(32)   NOT NULL,
    liked_etc           VARCHAR(500)  NULL,
    improvement_etc     VARCHAR(500)  NULL,
    free_comment        TEXT          NULL,
    submitted_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (session_feedback_id),
    CONSTRAINT uk_session_feedbacks_session_member UNIQUE (session_id, member_id),
    INDEX idx_session_feedbacks_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='세션 피드백 응답 (세션당 멤버 1건, 제출 후 수정 불가)';

COMMIT;

-- -----------------------------------------------------------------------------
-- [3] session_feedback_aspects
-- -----------------------------------------------------------------------------
START TRANSACTION;

CREATE TABLE IF NOT EXISTS session_feedback_aspects (
    session_feedback_aspect_id BIGINT      NOT NULL AUTO_INCREMENT,
    session_feedback_id        BIGINT      NOT NULL,
    question                   VARCHAR(32) NOT NULL,
    aspect                     VARCHAR(32) NOT NULL,
    PRIMARY KEY (session_feedback_aspect_id),
    INDEX idx_session_feedback_aspects_feedback_id (session_feedback_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='세션 피드백 선택형 항목 (좋았던 점/개선점의 선택 결과, 집계용)';

COMMIT;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
-- 테이블 생성 확인
SHOW TABLES LIKE 'session_feedback%';

-- session_feedback_forms 스키마 (기대: session_id UNIQUE, start_at/end_at NOT NULL, push_enabled NOT NULL)
SHOW CREATE TABLE session_feedback_forms;

-- session_feedbacks 스키마 (기대: (session_id, member_id) UNIQUE)
SHOW CREATE TABLE session_feedbacks;

-- session_feedback_aspects 스키마 (기대: session_feedback_id INDEX)
SHOW CREATE TABLE session_feedback_aspects;

-- 인덱스 확인
SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS, NON_UNIQUE
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('session_feedback_forms', 'session_feedbacks', 'session_feedback_aspects')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 신규 테이블 추가뿐이므로 DROP TABLE 만으로 충분하다.
-- 단, 롤백 전에 이 테이블을 매핑하는 애플리케이션 버전을 먼저 내려야 한다.
-- (ddl-auto: validate 로 인해 매핑 테이블이 없으면 기동 실패)
--
--   START TRANSACTION;
--   DROP TABLE IF EXISTS session_feedback_aspects;
--   DROP TABLE IF EXISTS session_feedbacks;
--   DROP TABLE IF EXISTS session_feedback_forms;
--   COMMIT;
-- =============================================================================
