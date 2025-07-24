-- Cohorts (기수)
INSERT INTO cohorts (cohort_id, created_at, updated_at, value) VALUES
                                                                   (1, 1710000000, 1710000000, '17'),
                                                                   (2, 1700000000, 1700000000, '16');

-- Teams (팀)
INSERT INTO teams (team_id, cohort_id, number, created_at, updated_at) VALUES
                                                                           (1, 1, 1, 1710000000, 1710000000),
                                                                           (2, 1, 2, 1710000000, 1710000000);

-- Members (회원)
INSERT INTO members (member_id, created_at, updated_at, deleted_at, email, name, part, status) VALUES
                                                                                                   (1, '2024-01-01 10:00:00', '2024-01-01 10:00:00', NULL, 'depromeetcore@gmail.com', '홍길동', 'WEB', 'ACTIVE'),
                                                                                                   (2, '2024-01-02 11:00:00', '2024-01-02 11:00:00', NULL, 'jane.doe@example.com', '제인', 'ANDROID', 'ACTIVE'),
                                                                                                   (3, '2024-01-03 12:00:00', '2024-01-03 12:00:00', NULL, 'john.smith@example.com', '존', 'SERVER', 'INACTIVE');

-- Member Cohorts (회원-기수 매핑)
INSERT INTO member_cohorts (member_cohort_id, member_id, cohort_id) VALUES
                                                                        (1, 1, 1),
                                                                        (2, 2, 1),
                                                                        (3, 3, 2);

-- Authorities (권한)
INSERT INTO authorities (authority_id, created_at, updated_at, name) VALUES
                                                                         (1, '2024-01-01 10:00:00', '2024-01-01 10:00:00', 'ROLE_USER'),
                                                                         (2, '2024-01-01 10:00:00', '2024-01-01 10:00:00', 'ROLE_ADMIN');

-- Member Authorities (회원-권한 매핑)
INSERT INTO member_authorities (member_authority_id, member_id, authority_id, granted_at, deleted_at) VALUES
                                                                                                          (1, 1, 1, '2024-01-01 10:00:00', NULL),
                                                                                                          (2, 2, 1, '2024-01-02 11:00:00', NULL),
                                                                                                          (3, 3, 2, '2024-01-03 12:00:00', NULL);

-- Sessions (세션)
INSERT INTO sessions (session_id, cohort_id, week, event_name, place, is_online, date, attendance_start, attendance_code) VALUES
                                                                                                                              (1, 1, 1, '디프만 17기 OT', '공덕 프론트원', b'0', '2025-08-02 14:00:00', '2025-08-02 14:00:00', '3821'),
                                                                                                                              (2, 1, 2, '미니 디프콘', '공덕 프론트원', b'0', '2025-08-09 14:00:00', '2025-08-09 14:00:00', '1234');

-- Attendances (출석)
INSERT INTO attendances (id, member_id, session_id, attended_at, status) VALUES
                                                                             (1, 1, 1, '2025-08-02 14:01:00', 'PRESENT'),
                                                                             (2, 2, 1, '2025-08-02 14:05:00', 'LATE'),
                                                                             (3, 3, 1, NULL, 'ABSENT'),
                                                                             (4, 1, 2, '2025-08-09 14:00:00', 'PRESENT');

-- Bill Accounts (정산 계좌)
INSERT INTO bill_accounts (bill_account_id, created_at, updated_at, deleted_at, account_holder_name, account_type, bank_name, bill_account_value) VALUES
    (1, '2024-01-01 10:00:00', '2024-01-01 10:00:00', NULL, '홍길동', '개인', 'KB 국민', '3333-12-1234567');

-- Bills (정산)
INSERT INTO bills (bill_id, bill_account_id, created_at, updated_at, deleted_at, host_user_id, bill_status, title, description, completed_at) VALUES
    (1, 1, '2024-01-01 10:00:00', '2024-01-01 10:00:00', NULL, 1, '정산 완료', '1차 회식 정산', '1차 회식에 대한 정산입니다.', '2024-01-10 10:00:00');

-- Gatherings (회식)
INSERT INTO gatherings (gathering_id, bill_id, created_at, updated_at, deleted_at, host_user_id, round_number, held_at, category, title, description) VALUES
    (1, 1, '2024-01-01 10:00:00', '2024-01-01 10:00:00', NULL, 1, 1, '2024-01-05 19:00:00', '회식', '1차 회식', '첫 번째 회식입니다.');

-- Gathering Members (회식 멤버)
INSERT INTO gathering_members (gathering_member_id, gathering_id, member_id, is_checked, is_joined, created_at, updated_at, deleted_at, completed_at) VALUES
                                                                                                                                                          (1, 1, 1, b'1', b'1', '2024-01-01 10:00:00', '2024-01-01 10:00:00', NULL, NULL),
                                                                                                                                                          (2, 1, 2, b'0', b'1', '2024-01-01 10:00:00', '2024-01-01 10:00:00', NULL, NULL);

-- Gathering Receipts (회식 영수증)
INSERT INTO gathering_receipts (receipt_id, gathering_id, amount, split_amount, created_at, updated_at, deleted_at) VALUES
    (1, 1, 100000, 50000, '2024-01-05 20:00:00', '2024-01-05 20:00:00', NULL);

-- Gathering Receipt Photos (영수증 사진)
INSERT INTO gathering_receipt_photos (receipt_photo_id, receipt_id, url, created_at, updated_at, deleted_at) VALUES
    (1, 1, 'https://example.com/receipt1.jpg', '2024-01-05 20:10:00', '2024-01-05 20:10:00', NULL);

-- Member OAuth (소셜 로그인)
INSERT INTO member_oauth (member_oauth_id, member_id, external_id, provider) VALUES
    (1, 1, 'oauth-123', 'google');

-- Member Teams (회원-팀 매핑)
INSERT INTO member_teams (member_team_id, member_id, team_id) VALUES
                                                                  (1, 1, 1),
                                                                  (2, 2, 2);

-- Refresh Tokens (리프레시 토큰)
INSERT INTO refresh_tokens (member_id, token) VALUES
                                                  (1, 'sample-refresh-token-1'),
                                                  (2, 'sample-refresh-token-2');

-- Session Attachments (세션 첨부파일)
INSERT INTO session_attachments (session_attachment_id, session_id, idx, path, title) VALUES
    (1, 1, 1, '/files/ot.pdf', 'OT 자료');
