-- =============================================================================
-- 2026-10-04 · 이미지 직접 업로드 세션 테이블 image_uploads (#580)
-- =============================================================================
-- 대상: MySQL 8
-- 목적: 프론트가 OCI 로 직접 올리는 업로드의 소유자/선언 형식·크기/쓰기 PAR/검증·복사 상태를 기록한다.
--       검증과 확정 키 복사가 끝난 뒤에만 images 행을 만들고 image_id 로 잇는다.
-- 선행: 2610021200_images.sql / 후행: 없음
-- 주의: 추가만 한다(기존 테이블 변경 없음). ddl-auto: validate 라 이 배포 전에 적용해야 한다(없으면 기동 실패).
--       여러 번 실행해도 안전하다. 객체 키는 upload_id 로 정해 저장하지 않는다(uploads/{id}, images/{id}).
--       PAR URL 은 저장하지 않는다(par_id 는 회수용 식별자). member FK 는 다른 테이블과 같이 두지 않는다.

CREATE TABLE IF NOT EXISTS image_uploads (
    upload_id       VARCHAR(36)  NOT NULL COMMENT '업로드 id (UUID, 클라이언트에 노출)',
    owner_member_id BIGINT       NOT NULL COMMENT '업로드한 회원',
    content_type    VARCHAR(20)  NOT NULL COMMENT '요청한 MIME (image/jpeg, image/png). 실제 내용과 같아야 통과',
    size_bytes      BIGINT       NOT NULL COMMENT '요청한 바이트 수. 실제 크기와 같아야 통과',
    par_id          VARCHAR(255) NULL     COMMENT '쓰기 PAR id. COMPLETED 에서 PAR 회수·업로드 객체 삭제가 끝나면 NULL',
    status          VARCHAR(20)  NOT NULL COMMENT 'PENDING/VERIFYING/COPYING/COMPLETED/REJECTED/FAILED/EXPIRED',
    expires_at      DATETIME(6)  NOT NULL COMMENT '업로드 URL 만료. 이후 새 검증을 시작하지 않음',
    lease_token     VARCHAR(36)  NULL     COMMENT '검증/복사 처리 주체. 상태 변경은 이 값이 일치할 때만',
    lease_until     DATETIME(6)  NULL     COMMENT '처리 lease 만료. 지나면 다른 요청이 이어받음',
    etag            VARCHAR(255) NULL     COMMENT '검증한 업로드 객체 ETag. 복사의 원본 조건',
    work_request_id VARCHAR(255) NULL     COMMENT 'OCI 복사 work request id',
    image_id        BIGINT       NULL     COMMENT '완료 시 만든 images.image_id',
    failure_code    VARCHAR(50)  NULL     COMMENT 'REJECTED/FAILED 사유(IMAGE 오류 코드 이름)',
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (upload_id),
    CONSTRAINT uk_image_uploads_image_id UNIQUE (image_id),
    INDEX idx_image_uploads_owner_member_id (owner_member_id),
    INDEX idx_image_uploads_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
-- 기대: 위 14개 컬럼. par_id, lease_token, lease_until, etag, work_request_id, image_id, failure_code 만 NULL 허용
SHOW COLUMNS FROM image_uploads;

-- 기대: PRIMARY(upload_id), uk_image_uploads_image_id(UNIQUE), idx_image_uploads_owner_member_id, idx_image_uploads_created_at
SHOW INDEX FROM image_uploads;

-- 운영 중 상태 분포(정리가 밀리는지 확인)
SELECT status, COUNT(*) AS uploads, MIN(created_at) AS oldest, SUM(par_id IS NOT NULL) AS cleanup_pending
FROM image_uploads
GROUP BY status;

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 직접 업로드 기능을 되돌린 뒤에만 실행한다. images 행과 확정 객체(images/)는 그대로 남는다.
-- 버킷의 uploads/ 객체는 지워지지 않으므로 필요하면 별도로 정리한다.
--
--  DROP TABLE IF EXISTS image_uploads;
-- =============================================================================
