SET NAMES utf8mb4;

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'MENU', 'iam:department:list', '部门管理', '/iam/departments', 'iam/departments/index', 'network', 204, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p WHERE p.code = 'iam:directory'
AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:department:list');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:department:create', '新增部门', NULL, NULL, NULL, 20401, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p WHERE p.code = 'iam:department:list'
AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:department:create');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:department:update', '编辑部门', NULL, NULL, NULL, 20402, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p WHERE p.code = 'iam:department:list'
AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:department:update');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:department:status', '启停部门', NULL, NULL, NULL, 20403, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p WHERE p.code = 'iam:department:list'
AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:department:status');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:department:delete', '删除部门', NULL, NULL, NULL, 20404, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p WHERE p.code = 'iam:department:list'
AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:department:delete');
