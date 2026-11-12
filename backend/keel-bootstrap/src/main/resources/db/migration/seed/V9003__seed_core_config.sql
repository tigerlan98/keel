SET NAMES utf8mb4;

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'system.name', 'Keel', 'SYSTEM', 1, '系统名称', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'system.name');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'system.logo-url', '', 'SYSTEM', 1, '系统 Logo 地址', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'system.logo-url');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'auth.web.idle-timeout-minutes', '30', 'AUTH', 0, 'Web 空闲超时（分钟）', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'auth.web.idle-timeout-minutes');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'auth.web.absolute-timeout-hours', '12', 'AUTH', 0, 'Web 绝对超时（小时）', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'auth.web.absolute-timeout-hours');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'auth.app.idle-timeout-days', '7', 'AUTH', 0, 'App 空闲超时（天）', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'auth.app.idle-timeout-days');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'auth.app.absolute-timeout-days', '30', 'AUTH', 0, 'App 绝对超时（天）', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'auth.app.absolute-timeout-days');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'auth.max-login-failures', '5', 'AUTH', 0, '连续登录失败上限', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'auth.max-login-failures');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'auth.lock-minutes', '15', 'AUTH', 0, '登录锁定时长（分钟）', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'auth.lock-minutes');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'audit.operation.retention-days', '180', 'AUDIT', 0, '操作日志保留天数', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'audit.operation.retention-days');

INSERT INTO sys_config (config_key, config_value, config_group, public_flag, description, created_at, created_by, updated_at, updated_by, version)
SELECT 'audit.login.retention-days', '365', 'AUDIT', 0, '登录日志保留天数', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'audit.login.retention-days');
