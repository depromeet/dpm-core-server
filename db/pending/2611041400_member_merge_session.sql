-- Apply before deploying the server. No historical sessions are restored.
-- Preflight: must return zero rows. Investigate duplicates before running ALTER.
SELECT source_member_id, COUNT(*) FROM member_merge_events
GROUP BY source_member_id HAVING COUNT(*) > 1;
-- This fails rather than discarding data when duplicate sources exist.
ALTER TABLE member_merge_events
    ADD UNIQUE KEY uk_member_merge_events_source (source_member_id);
