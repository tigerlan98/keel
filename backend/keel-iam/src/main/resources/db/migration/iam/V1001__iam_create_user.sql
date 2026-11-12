SET NAMES utf8mb4;

CREATE TABLE iam_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(64) NOT NULL COMMENT '登录名',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
  nickname VARCHAR(64) NOT NULL DEFAULT '' COMMENT '昵称',
  email VARCHAR(128) NULL COMMENT '邮箱，可为空',
  phone VARCHAR(32) NULL COMMENT '手机号，可为空',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  must_change_password TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须修改密码：0否1是',
  last_login_at DATETIME(3) NULL COMMENT '最近登录时间',
  last_login_ip VARCHAR(64) NULL COMMENT '最近登录IP',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID，0=系统',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
  updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人ID，0=系统',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iam_user_username (username),
  KEY idx_iam_user_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IAM 用户';
