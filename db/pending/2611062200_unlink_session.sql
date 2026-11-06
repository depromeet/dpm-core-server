-- Apply before deploying the server. Existing sessions acquire a binding on reissue.
SET @ddl := IF(EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'refresh_tokens'
      AND column_name = 'continuation_member_id'
), 'SELECT 1', 'ALTER TABLE refresh_tokens ADD COLUMN continuation_member_id BIGINT NULL');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
