SET NAMES utf8mb4;

INSERT INTO iam_resource (parent_id, type, code, name, path, component, icon, sort, visible, status, created_at, created_by, updated_at, updated_by, version)
SELECT p.id, 'BUTTON', 'iam:user:delete', '删除用户', NULL, NULL, NULL, 20107, 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
FROM iam_resource p
WHERE p.code = 'iam:user:list'
  AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE code = 'iam:user:delete');
