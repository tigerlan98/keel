SET NAMES utf8mb4;

-- 仅初始化角色资料；不自动授予资源，不关联用户，不覆盖已存在角色。
INSERT INTO iam_role
    (code, name, description, status, sort, created_at, created_by, updated_at, updated_by, version)
SELECT seed.code, seed.name, seed.description, 'ENABLED', seed.sort,
       NOW(3), 0, NOW(3), 0, 0
FROM (
    SELECT 'SYS_MANAGER' AS code, '系统管理员' AS name,
           '系统管理职责，权限按需分配' AS description, 2 AS sort
    UNION ALL
    SELECT 'SYS_OPERATOR', '运营专员', '运营业务职责，权限按需分配', 3
    UNION ALL
    SELECT 'SYS_AUDITOR', '审计员', '审计查看职责，权限按需分配', 4
    UNION ALL
    SELECT 'SYS_GUEST', '访客', '访客查看职责，权限按需分配', 5
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM iam_role existing WHERE existing.code = seed.code);
