-- Apply before deploying the server. Existing sessions acquire a binding on reissue.
ALTER TABLE refresh_tokens ADD COLUMN continuation_member_id BIGINT NULL;
