-- =============================================================================
-- 2026-10-05 · 이미지 원본 파일명 컬럼 추가 (#585)
-- =============================================================================
-- 대상: MySQL 8
-- 목적: 업로드 요청에 받은 원본 파일명을 업로드 세션(image_uploads)에 두었다가 완료 시 images 로 옮겨 저장한다.
--       결석 사유서 첨부 조회에서 imageId 와 함께 파일명을 돌려준다.
-- 선행: 2610021200_images.sql, 2610041200_image_uploads.sql / 후행: 없음
-- 주의: ddl-auto: validate 라 이 배포 전에 적용해야 한다(없으면 기동 실패). 여러 번 실행해도 안전하다.
--       기존 행은 파일명을 받은 적이 없어 NULL 로 둔다(채울 원본이 없음).

-- -----------------------------------------------------------------------------
-- [1] images.original_file_name
-- -----------------------------------------------------------------------------
SET @has_images_name := (SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = DATABASE() AND table_name = 'images'
                           AND column_name = 'original_file_name');
SET @sql1 := IF(@has_images_name = 0,
    'ALTER TABLE images ADD COLUMN original_file_name VARCHAR(255) NULL COMMENT ''업로드 요청에 받은 원본 파일명(표시용). 없으면 NULL'' AFTER size_bytes',
    'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

-- -----------------------------------------------------------------------------
-- [2] image_uploads.original_file_name
-- -----------------------------------------------------------------------------
SET @has_uploads_name := (SELECT COUNT(*) FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND table_name = 'image_uploads'
                            AND column_name = 'original_file_name');
SET @sql2 := IF(@has_uploads_name = 0,
    'ALTER TABLE image_uploads ADD COLUMN original_file_name VARCHAR(255) NULL COMMENT ''업로드 요청에 받은 원본 파일명. 완료 시 images 로 옮김'' AFTER size_bytes',
    'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;

-- =============================================================================
-- VERIFY (읽기 전용)
-- =============================================================================
-- 기대: 두 테이블 모두 original_file_name / varchar(255) / YES
SHOW COLUMNS FROM images LIKE 'original_file_name';
SHOW COLUMNS FROM image_uploads LIKE 'original_file_name';

-- =============================================================================
-- ROLLBACK
-- =============================================================================
-- 파일명 저장 기능을 되돌린 뒤에만 실행한다. 저장된 파일명은 사라진다.
--
--  ALTER TABLE images DROP COLUMN original_file_name;
--  ALTER TABLE image_uploads DROP COLUMN original_file_name;
-- =============================================================================
