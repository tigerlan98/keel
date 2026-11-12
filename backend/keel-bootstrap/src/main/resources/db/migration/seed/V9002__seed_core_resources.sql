SET NAMES utf8mb4;

-- 目录
INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT 0, 'DIRECTORY', 'iam:directory', '身份与权限', '/iam', NULL, 'shield', 20, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:directory');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT 0, 'DIRECTORY', 'sys:directory', '系统设置', '/system', NULL, 'settings', 30, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'sys:directory');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT 0, 'DIRECTORY', 'audit:directory', '审计日志', '/audit', NULL, 'file-clock', 40, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'audit:directory');

-- 菜单
INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'MENU', 'iam:user:list', '用户管理', '/iam/users', 'iam/users/index', 'users', 201, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:directory'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:user:list');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'MENU', 'iam:role:list', '角色管理', '/iam/roles', 'iam/roles/index', 'key-round', 202, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:directory'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:role:list');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'MENU', 'iam:resource:list', '资源管理', '/iam/resources', 'iam/resources/index', 'menu', 203, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:directory'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:resource:list');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'MENU', 'sys:config:list', '系统参数', '/system/config', 'system/config/index', 'sliders-horizontal', 301, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'sys:directory'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'sys:config:list');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'MENU', 'audit:operation:list', '操作日志', '/audit/operation-logs', 'audit/operation-logs/index', 'scroll-text', 401, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'audit:directory'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'audit:operation:list');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'MENU', 'audit:login:list', '登录日志', '/audit/login-logs', 'audit/login-logs/index', 'log-in', 402, 1, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'audit:directory'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'audit:login:list');

-- 用户管理按钮
INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:user:create', '新增用户', NULL, NULL, NULL, 20101, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:user:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:user:create');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:user:update', '编辑用户', NULL, NULL, NULL, 20102, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:user:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:user:update');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:user:status', '启用/禁用用户', NULL, NULL, NULL, 20103, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:user:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:user:status');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:user:reset-password', '重置密码', NULL, NULL, NULL, 20104, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:user:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:user:reset-password');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:user:assign-role', '分配角色', NULL, NULL, NULL, 20105, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:user:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:user:assign-role');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:user:force-logout', '强制下线', NULL, NULL, NULL, 20106, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:user:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:user:force-logout');

-- 角色管理按钮
INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:role:create', '新增角色', NULL, NULL, NULL, 20201, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:role:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:role:create');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:role:update', '编辑角色', NULL, NULL, NULL, 20202, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:role:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:role:update');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:role:delete', '删除角色', NULL, NULL, NULL, 20203, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:role:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:role:delete');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:role:assign-resource', '分配资源', NULL, NULL, NULL, 20204, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:role:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:role:assign-resource');

-- 资源管理按钮
INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:resource:update', '编辑资源', NULL, NULL, NULL, 20301, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:resource:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:resource:update');

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:resource:status', '启用/禁用资源', NULL, NULL, NULL, 20302, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:resource:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:resource:status');

-- 系统参数按钮
INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'sys:config:update', '修改参数', NULL, NULL, NULL, 30101, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'sys:config:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'sys:config:update');
