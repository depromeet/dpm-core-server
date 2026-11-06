-- Apply before deploying the server. Existing sessions acquire a binding on reissue.
-- Run with the MySQL client and CREATE/ALTER ROUTINE privileges.
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_unlink_session_2611062200$$
CREATE PROCEDURE migrate_unlink_session_2611062200()
SQL SECURITY INVOKER
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'refresh_tokens'
          AND column_name = 'continuation_member_id'
    ) THEN
        ALTER TABLE refresh_tokens ADD COLUMN continuation_member_id BIGINT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'refresh_tokens'
          AND column_name = 'continuation_member_id'
          AND column_type = 'bigint' AND is_nullable = 'YES'
          AND column_default IS NULL AND extra = ''
    ) THEN
        SELECT column_type, is_nullable, column_default, extra
        FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'refresh_tokens'
          AND column_name = 'continuation_member_id';

        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Expected refresh_tokens.continuation_member_id to be a writable signed BIGINT NULL DEFAULT NULL';
    END IF;
END$$
CALL migrate_unlink_session_2611062200()$$
DROP PROCEDURE migrate_unlink_session_2611062200$$
DELIMITER ;
