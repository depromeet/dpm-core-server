-- =============================================================================
-- 2026-10-02 · 이미지 메타데이터 테이블 images (#580)
-- =============================================================================
-- 대상: MySQL 8
-- 목적: OCI Object Storage 비공개 버킷에 올린 이미지의 소유자/키/형식/크기를 기록한다.
-- 선행: 없음 / 후행: 없음
-- 주의: ddl-auto: validate 라 이미지 기능 배포 전에 적용해야 한다(없으면 기동 실패). 여러 번 실행해도 안전하다.
--       object_key 는 서버가 만든 UUID 라 외부에 노출하지 않는다. member FK 는 다른 테이블과 같이 두지 않는다.

CREATE TABLE IF NOT EXISTS images (
    image_id        BIGINT       NOT NULL AUTO_INCREMENT,
    owner_member_id BIGINT       NOT NULL COMMENT '업로드한 회원',
    object_key      VARCHAR(100) NOT NULL COMMENT '버킷 내 객체 이름 (images/{UUID})',
    content_type    VARCHAR(20)  NOT NULL COMMENT '내용으로 판별한 MIME (image/jpeg, image/png)',
    size_bytes      BIGINT       NOT NULL COMMENT '바이트 크기',
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (image_id),
    CONSTRAINT uk_images_object_key UNIQUE (object_key),
    INDEX idx_images_owner_member_id (owner_member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
-- 기대: image_id bigint / owner_member_id bigint / object_key varchar(100) / content_type varchar(20)
--       / size_bytes bigint / created_at datetime(6), 모두 NOT NULL
SHOW COLUMNS FROM images;

-- 기대: PRIMARY, uk_images_object_key(UNIQUE), idx_images_owner_member_id
SHOW INDEX FROM images;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 이미지 기능을 되돌린 뒤에만 실행한다. 버킷의 객체는 지워지지 않으므로 필요하면 별도로 정리한다.
--
--  DROP TABLE IF EXISTS images;
-- =============================================================================
