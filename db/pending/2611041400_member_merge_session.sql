-- Apply before deploying the server. No historical sessions are restored.
-- Preflight: must return zero rows. Investigate duplicates before running ALTER.
SELECT source_member_id, COUNT(*) FROM member_merge_events
GROUP BY source_member_id HAVING COUNT(*) > 1;
-- This fails rather than discarding data when duplicate sources exist.
ALTER TABLE member_merge_events
    ADD UNIQUE KEY uk_member_merge_events_source (source_member_id);

-- Expand before deployment. Pause refresh/login/merge writes during server cutover:
-- old servers do not honor the successor pointer and can fork rotation chains.
-- Existing rows remain valid; their first rotation on the new server records a successor.
ALTER TABLE refresh_tokens
    ADD COLUMN token_jti VARCHAR(36) NULL,
    ADD COLUMN issued_member_id BIGINT NULL,
    ADD COLUMN successor_token_hash VARCHAR(64) NULL;
-- No raw JWT is written to the legacy token column by the new server.
-- Do not roll back to a plaintext-only token lookup implementation.
