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
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (3,1),(3,2),(3,3),(3,4),(3,5),(3,6),(3,7),(3,8),
                                                          (3,9),(3,10),(3,11),(3,12),(3,13),(3,14),(3,15),(3,16),
                                                          (3,17),(3,18),(3,19),(3,20),(3,21),(3,22),(3,23),(3,24),
                                                          (3,25),(3,26),(3,27),(3,28),(3,29),(3,30),(3,31),(3,32);
-- 18기 운영진
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (4,1),(4,2),(4,3),(4,4),(4,5),(4,6),(4,7),(4,8),
                                                          (4,9),(4,10),(4,11),(4,12),(4,13),(4,14),(4,15),(4,16),
                                                          (4,17),(4,18),(4,19),(4,20),(4,21),(4,22),(4,23),(4,24),
                                                          (4,25),(4,26),(4,27),(4,28),(4,29),(4,30),(4,31),(4,32);

-- 코어 1기
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (6,1),(6,2),(6,3),(6,4),(6,5),(6,6),(6,7),(6,8),
                                                          (6,9),(6,10),(6,11),(6,12),(6,13),(6,14),(6,15),(6,16),
                                                          (6,17),(6,18),(6,19),(6,20),(6,21),(6,22),(6,23),(6,24),
                                                          (6,25),(6,26),(6,27),(6,28),(6,29),(6,30),(6,31),(6,32);

-- 코어 2기
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (7,1),(7,2),(7,3),(7,4),(7,5),(7,6),(7,7),(7,8),
                                                          (7,9),(7,10),(7,11),(7,12),(7,13),(7,14),(7,15),(7,16),
                                                          (7,17),(7,18),(7,19),(7,20),(7,21),(7,22),(7,23),(7,24),
                                                          (7,25),(7,26),(7,27),(7,28),(7,29),(7,30),(7,31),(7,32);

-- 17기 디퍼
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (1,3),(1,7),(1,11),(1,15),
                                                          (1,19),(1,23),(1,27),(1,31);

-- 18기 디퍼
INSERT INTO role_permissions (role_id, permission_id) VALUES
                                                          (2,3),(2,7),(2,11),(2,15),
                                                          (2,19),(2,23),(2,27),(2,31);
