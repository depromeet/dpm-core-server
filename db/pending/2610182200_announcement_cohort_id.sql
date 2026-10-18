-- 2026-10-18 / 공지에 cohort_id 추가
-- ※ 아래 구문을 하나씩 개별 실행한다 (전체 선택 후 실행 X)
--   DBeaver: 구문에 커서 두고 Ctrl+Enter
--   MySQL CLI: 구문 복사 후 개별 입력

-- [1] cohort_id 컬럼 추가 (이미 있으면 "Duplicate column" 오류 무시)
ALTER TABLE announcements ADD COLUMN cohort_id BIGINT NULL;

-- [2] 기존 공지를 18기로 백필
UPDATE announcements a
JOIN cohorts c ON c.value = '18'
SET a.cohort_id = c.cohort_id
WHERE a.cohort_id IS NULL;

-- [3] NOT NULL로 전환
ALTER TABLE announcements MODIFY COLUMN cohort_id BIGINT NOT NULL;

-- [4] 인덱스 추가 (이미 있으면 "Duplicate key" 오류 무시)
CREATE INDEX idx_announcements_cohort_id ON announcements (cohort_id);

-- VERIFY
SELECT COUNT(*) AS null_count FROM announcements WHERE cohort_id IS NULL;

SELECT c.value, COUNT(*) AS announcement_count
FROM announcements a
JOIN cohorts c ON c.cohort_id = a.cohort_id
GROUP BY c.value
ORDER BY c.value;
