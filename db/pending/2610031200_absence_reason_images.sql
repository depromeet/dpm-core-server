-- =============================================================================
-- 2026-10-03 · 결석 사유서 첨부 이미지 링크 테이블 absence_reason_images (#583)
-- =============================================================================
-- 대상: MySQL 8
-- 목적: 결석 사유서 1건에 이미지 N장을 순서대로 붙인다. UNIQUE(image_id) 로 이미지 하나는 한 사유서에만 붙는다.
-- 선행: 2610021200_images.sql / 후행: 없음
-- 주의: ddl-auto: validate 라 배포 전에 적용해야 한다(없으면 기동 실패). 여러 번 실행해도 안전하다.
--       다른 테이블과 같이 FK 는 두지 않는다. 링크를 지워도 images 행과 버킷 객체는 남는다.

CREATE TABLE IF NOT EXISTS absence_reason_images (
    absence_reason_image_id BIGINT NOT NULL AUTO_INCREMENT,
    absence_reason_id       BIGINT NOT NULL COMMENT 'absence_reasons.absence_reason_id',
    image_id                BIGINT NOT NULL COMMENT 'images.image_id',
    display_order           INT    NOT NULL COMMENT '0부터 시작하는 표시 순서',
    PRIMARY KEY (absence_reason_image_id),
    CONSTRAINT uk_absence_reason_images_image_id UNIQUE (image_id),
    INDEX idx_absence_reason_images_reason_order (absence_reason_id, display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
-- 기대: absence_reason_image_id bigint / absence_reason_id bigint / image_id bigint / display_order int, 모두 NOT NULL
SHOW COLUMNS FROM absence_reason_images;

-- 기대: PRIMARY, uk_absence_reason_images_image_id(Non_unique=0, image_id),
--       idx_absence_reason_images_reason_order(absence_reason_id, display_order)
SHOW INDEX FROM absence_reason_images;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 이 테이블을 쓰지 않는 이전 버전 애플리케이션으로 먼저 되돌린다(테이블부터 지우면 validate 로 기동 실패).
-- 첨부 관계만 사라지며 images 와 버킷 객체는 그대로 남는다.
--
--  DROP TABLE IF EXISTS absence_reason_images;
