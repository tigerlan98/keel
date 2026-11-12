SET NAMES utf8mb4;

INSERT INTO iam_role (code, name, description, status, sort, created_at, created_by, updated_at, updated_by, version)
SELECT 'SUPER_ADMIN', '超级管理员', '内置超级管理员角色', 'ENABLED', 0, NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_role WHERE code = 'SUPER_ADMIN');
