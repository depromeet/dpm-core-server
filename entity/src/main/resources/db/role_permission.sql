INSERT INTO roles (role_id, name) VALUES (1, '17기 디퍼');
INSERT INTO roles (role_id, name) VALUES (2, '18기 디퍼');
INSERT INTO roles (role_id, name) VALUES (3, '17기 운영진');
INSERT INTO roles (role_id, name) VALUES (4, '18기 운영진');
INSERT INTO roles (role_id, name) VALUES (5, '게스트');
INSERT INTO roles (role_id, name) VALUES (6, '코어 1기');
INSERT INTO roles (role_id, name) VALUES (7, '코어 2기');

INSERT INTO permissions (permission_id, action, resource) VALUES (1, 'CREATE', 'ATTENDANCE');
INSERT INTO permissions (permission_id, action, resource) VALUES (2, 'DELETE', 'ATTENDANCE');
INSERT INTO permissions (permission_id, action, resource) VALUES (3, 'READ', 'ATTENDANCE');
INSERT INTO permissions (permission_id, action, resource) VALUES (4, 'UPDATE', 'ATTENDANCE');
INSERT INTO permissions (permission_id, action, resource) VALUES (5, 'CREATE', 'AUTHORIZATION');
INSERT INTO permissions (permission_id, action, resource) VALUES (6, 'DELETE', 'AUTHORIZATION');
INSERT INTO permissions (permission_id, action, resource) VALUES (7, 'READ', 'AUTHORIZATION');
INSERT INTO permissions (permission_id, action, resource) VALUES (8, 'UPDATE', 'AUTHORIZATION');
INSERT INTO permissions (permission_id, action, resource) VALUES (9, 'CREATE', 'BILL');
INSERT INTO permissions (permission_id, action, resource) VALUES (10, 'DELETE', 'BILL');
INSERT INTO permissions (permission_id, action, resource) VALUES (11, 'READ', 'BILL');
INSERT INTO permissions (permission_id, action, resource) VALUES (12, 'UPDATE', 'BILL');
INSERT INTO permissions (permission_id, action, resource) VALUES (13, 'CREATE', 'COHORT');
INSERT INTO permissions (permission_id, action, resource) VALUES (14, 'DELETE', 'COHORT');
INSERT INTO permissions (permission_id, action, resource) VALUES (15, 'READ', 'COHORT');
INSERT INTO permissions (permission_id, action, resource) VALUES (16, 'UPDATE', 'COHORT');
INSERT INTO permissions (permission_id, action, resource) VALUES (17, 'CREATE', 'GATHERING');
INSERT INTO permissions (permission_id, action, resource) VALUES (18, 'DELETE', 'GATHERING');
INSERT INTO permissions (permission_id, action, resource) VALUES (19, 'READ', 'GATHERING');
INSERT INTO permissions (permission_id, action, resource) VALUES (20, 'UPDATE', 'GATHERING');
INSERT INTO permissions (permission_id, action, resource) VALUES (21, 'CREATE', 'MEMBER');
INSERT INTO permissions (permission_id, action, resource) VALUES (22, 'DELETE', 'MEMBER');
INSERT INTO permissions (permission_id, action, resource) VALUES (23, 'READ', 'MEMBER');
INSERT INTO permissions (permission_id, action, resource) VALUES (24, 'UPDATE', 'MEMBER');
INSERT INTO permissions (permission_id, action, resource) VALUES (25, 'CREATE', 'SESSION');
INSERT INTO permissions (permission_id, action, resource) VALUES (26, 'DELETE', 'SESSION');
INSERT INTO permissions (permission_id, action, resource) VALUES (27, 'READ', 'SESSION');
INSERT INTO permissions (permission_id, action, resource) VALUES (28, 'UPDATE', 'SESSION');
INSERT INTO permissions (permission_id, action, resource) VALUES (29, 'CREATE', 'TEAM');
INSERT INTO permissions (permission_id, action, resource) VALUES (30, 'DELETE', 'TEAM');
INSERT INTO permissions (permission_id, action, resource) VALUES (31, 'READ', 'TEAM');
INSERT INTO permissions (permission_id, action, resource) VALUES (32, 'UPDATE', 'TEAM');

-- 17기 운영진
INSERT INTO role_permissions (role_id, permission_id, granted_at) VALUES
                                                                      (3,1,NOW(6)),(3,2,NOW(6)),(3,3,NOW(6)),(3,4,NOW(6)),(3,5,NOW(6)),(3,6,NOW(6)),(3,7,NOW(6)),(3,8,NOW(6)),
                                                                      (3,9,NOW(6)),(3,10,NOW(6)),(3,11,NOW(6)),(3,12,NOW(6)),(3,13,NOW(6)),(3,14,NOW(6)),(3,15,NOW(6)),(3,16,NOW(6)),
                                                                      (3,17,NOW(6)),(3,18,NOW(6)),(3,19,NOW(6)),(3,20,NOW(6)),(3,21,NOW(6)),(3,22,NOW(6)),(3,23,NOW(6)),(3,24,NOW(6)),
                                                                      (3,25,NOW(6)),(3,26,NOW(6)),(3,27,NOW(6)),(3,28,NOW(6)),(3,29,NOW(6)),(3,30,NOW(6)),(3,31,NOW(6)),(3,32,NOW(6));

-- 18기 운영진
INSERT INTO role_permissions (role_id, permission_id, granted_at) VALUES
                                                                      (4,1,NOW(6)),(4,2,NOW(6)),(4,3,NOW(6)),(4,4,NOW(6)),(4,5,NOW(6)),(4,6,NOW(6)),(4,7,NOW(6)),(4,8,NOW(6)),
                                                                      (4,9,NOW(6)),(4,10,NOW(6)),(4,11,NOW(6)),(4,12,NOW(6)),(4,13,NOW(6)),(4,14,NOW(6)),(4,15,NOW(6)),(4,16,NOW(6)),
                                                                      (4,17,NOW(6)),(4,18,NOW(6)),(4,19,NOW(6)),(4,20,NOW(6)),(4,21,NOW(6)),(4,22,NOW(6)),(4,23,NOW(6)),(4,24,NOW(6)),
                                                                      (4,25,NOW(6)),(4,26,NOW(6)),(4,27,NOW(6)),(4,28,NOW(6)),(4,29,NOW(6)),(4,30,NOW(6)),(4,31,NOW(6)),(4,32,NOW(6));

-- 코어 1기
INSERT INTO role_permissions (role_id, permission_id, granted_at) VALUES
                                                                      (6,1,NOW(6)),(6,2,NOW(6)),(6,3,NOW(6)),(6,4,NOW(6)),(6,5,NOW(6)),(6,6,NOW(6)),(6,7,NOW(6)),(6,8,NOW(6)),
                                                                      (6,9,NOW(6)),(6,10,NOW(6)),(6,11,NOW(6)),(6,12,NOW(6)),(6,13,NOW(6)),(6,14,NOW(6)),(6,15,NOW(6)),(6,16,NOW(6)),
                                                                      (6,17,NOW(6)),(6,18,NOW(6)),(6,19,NOW(6)),(6,20,NOW(6)),(6,21,NOW(6)),(6,22,NOW(6)),(6,23,NOW(6)),(6,24,NOW(6)),
                                                                      (6,25,NOW(6)),(6,26,NOW(6)),(6,27,NOW(6)),(6,28,NOW(6)),(6,29,NOW(6)),(6,30,NOW(6)),(6,31,NOW(6)),(6,32,NOW(6));

-- 코어 2기
INSERT INTO role_permissions (role_id, permission_id, granted_at) VALUES
                                                                      (7,1,NOW(6)),(7,2,NOW(6)),(7,3,NOW(6)),(7,4,NOW(6)),(7,5,NOW(6)),(7,6,NOW(6)),(7,7,NOW(6)),(7,8,NOW(6)),
                                                                      (7,9,NOW(6)),(7,10,NOW(6)),(7,11,NOW(6)),(7,12,NOW(6)),(7,13,NOW(6)),(7,14,NOW(6)),(7,15,NOW(6)),(7,16,NOW(6)),
                                                                      (7,17,NOW(6)),(7,18,NOW(6)),(7,19,NOW(6)),(7,20,NOW(6)),(7,21,NOW(6)),(7,22,NOW(6)),(7,23,NOW(6)),(7,24,NOW(6)),
                                                                      (7,25,NOW(6)),(7,26,NOW(6)),(7,27,NOW(6)),(7,28,NOW(6)),(7,29,NOW(6)),(7,30,NOW(6)),(7,31,NOW(6)),(7,32,NOW(6));

-- 17기 디퍼
INSERT INTO role_permissions (role_id, permission_id, granted_at) VALUES
                                                                      (1,3,NOW(6)),(1,7,NOW(6)),(1,11,NOW(6)),(1,15,NOW(6)),
                                                                      (1,19,NOW(6)),(1,23,NOW(6)),(1,27,NOW(6)),(1,31,NOW(6));

-- 18기 디퍼
INSERT INTO role_permissions (role_id, permission_id, granted_at) VALUES
                                                                      (2,3,NOW(6)),(2,7,NOW(6)),(2,11,NOW(6)),(2,15,NOW(6)),
                                                                      (2,19,NOW(6)),(2,23,NOW(6)),(2,27,NOW(6)),(2,31,NOW(6));
