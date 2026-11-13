SET NAMES utf8mb4;

-- 开发脚手架默认超级管理员：admin（默认密码见 README / 部署文档）。
-- 生产部署必须首次登录后立即修改密码。
INSERT INTO iam_user (username, password_hash, nickname, status, must_change_password,
                      created_at, created_by, updated_at, updated_by, version)
SELECT 'admin',
       '$2a$10$9d0xn4UKyg4pcFAt2./gYOL.prelJt09N24a2z.2wfeqGyt8jCjni',
       '超级管理员',
       'ENABLED',
       0,
       NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_user WHERE username = 'admin');

INSERT INTO iam_user_role (user_id, role_id, created_at, created_by)
SELECT u.id, r.id, NOW(3), 0
FROM iam_user u
JOIN iam_role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM iam_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
