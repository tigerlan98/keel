-- ============================================================
-- Keel 数据库结构基线（参考 DDL）
-- 与 docs/design/03-数据库设计.md 保持一致。
-- 实际部署以 Flyway 迁移为准；本文件仅用于评审、对照与初始化参考。
-- 不包含种子数据；种子见 Flyway V9001–V9003。
-- 迁移对应关系：V1001–V1005 iam、V2001 sys_config、V3001–V3002 audit。
-- 禁止物理外键：本文件不包含任何 FOREIGN KEY 约束。
-- 在目标 schema 中执行；本文不创建数据库。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- IAM 用户
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- IAM 角色
-- ------------------------------------------------------------
CREATE TABLE iam_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  code VARCHAR(64) NOT NULL COMMENT '角色编码，稳定标识',
  name VARCHAR(64) NOT NULL COMMENT '角色名称',
  description VARCHAR(255) NULL COMMENT '描述',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID，0=系统',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
  updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人ID，0=系统',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iam_role_code (code),
  KEY idx_iam_role_status_sort (status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IAM 角色';

-- ------------------------------------------------------------
-- IAM 资源（目录 / 菜单 / 按钮 / 接口）
-- ------------------------------------------------------------
CREATE TABLE iam_resource (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父资源ID，0=根',
  type VARCHAR(16) NOT NULL COMMENT '类型：DIRECTORY/MENU/BUTTON/API',
  code VARCHAR(128) NOT NULL COMMENT '资源编码；目录仅作稳定标识，菜单/按钮/接口用于鉴权',
  name VARCHAR(64) NOT NULL COMMENT '资源名称',
  path VARCHAR(255) NULL COMMENT '前端路径，目录/菜单使用',
  component VARCHAR(255) NULL COMMENT '前端组件，菜单使用',
  icon VARCHAR(64) NULL COMMENT '图标名',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  visible TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否显示：0否1是',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID，0=系统',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
  updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人ID，0=系统',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iam_resource_code (code),
  UNIQUE KEY uk_iam_resource_path (path),
  KEY idx_iam_resource_parent_id (parent_id),
  KEY idx_iam_resource_type_status_sort (type, status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IAM 资源';

-- ------------------------------------------------------------
-- IAM 用户-角色关联
-- ------------------------------------------------------------
CREATE TABLE iam_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID，0=系统',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iam_user_role_user_id_role_id (user_id, role_id),
  KEY idx_iam_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IAM 用户角色关联';

-- ------------------------------------------------------------
-- IAM 角色-资源关联
-- ------------------------------------------------------------
CREATE TABLE iam_role_resource (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  resource_id BIGINT NOT NULL COMMENT '资源ID',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID，0=系统',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iam_role_resource_role_id_resource_id (role_id, resource_id),
  KEY idx_iam_role_resource_resource_id (resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IAM 角色资源关联';

-- ------------------------------------------------------------
-- 系统参数
-- ------------------------------------------------------------
CREATE TABLE sys_config (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_key VARCHAR(128) NOT NULL COMMENT '参数键',
  config_value TEXT NOT NULL COMMENT '参数值',
  config_group VARCHAR(64) NOT NULL DEFAULT 'DEFAULT' COMMENT '参数分组',
  public_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开：0否1是',
  description VARCHAR(255) NULL COMMENT '说明',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID，0=系统',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
  updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人ID，0=系统',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_config_key (config_key),
  KEY idx_sys_config_group_config_key (config_group, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统参数';

-- ------------------------------------------------------------
-- 操作日志（只追加）
-- ------------------------------------------------------------
CREATE TABLE aud_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  trace_id VARCHAR(64) NULL COMMENT '请求链路标识',
  user_id BIGINT NULL COMMENT '操作人ID，系统任务为空',
  username VARCHAR(64) NULL COMMENT '操作人用户名',
  module VARCHAR(32) NOT NULL COMMENT '模块编码',
  action VARCHAR(32) NOT NULL COMMENT '动作编码',
  method VARCHAR(255) NULL COMMENT '类名#方法名',
  request_method VARCHAR(8) NULL COMMENT 'HTTP 方法',
  request_uri VARCHAR(512) NULL COMMENT '请求 URI',
  request_params TEXT NULL COMMENT '脱敏并截断后的请求参数',
  success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否成功：0否1是',
  error_message VARCHAR(1024) NULL COMMENT '错误信息',
  ip VARCHAR(64) NULL COMMENT '来源IP',
  user_agent VARCHAR(512) NULL COMMENT 'User-Agent',
  duration_ms INT NULL COMMENT '耗时毫秒',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_aud_operation_log_created_at (created_at),
  KEY idx_aud_operation_log_user_id_created_at (user_id, created_at),
  KEY idx_aud_operation_log_username_created_at (username, created_at),
  KEY idx_aud_operation_log_module_action_created_at (module, action, created_at),
  KEY idx_aud_operation_log_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志';

-- ------------------------------------------------------------
-- 登录日志（只追加）
-- ------------------------------------------------------------
CREATE TABLE aud_login_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  trace_id VARCHAR(64) NULL COMMENT '请求链路标识',
  user_id BIGINT NULL COMMENT '用户ID，未知用户为空',
  username VARCHAR(64) NOT NULL COMMENT '登录名',
  action VARCHAR(16) NOT NULL COMMENT '动作：LOGIN/LOGOUT/LOGIN_FAILED',
  login_type VARCHAR(16) NOT NULL DEFAULT 'ADMIN' COMMENT '账号体系：ADMIN/APP/MINI',
  device VARCHAR(16) NOT NULL DEFAULT 'WEB' COMMENT '终端：WEB/APP/MINI',
  ip VARCHAR(64) NULL COMMENT '来源IP',
  user_agent VARCHAR(512) NULL COMMENT 'User-Agent',
  success TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否成功：0否1是',
  failure_reason VARCHAR(64) NULL COMMENT '失败原因编码',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_aud_login_log_created_at (created_at),
  KEY idx_aud_login_log_username_created_at (username, created_at),
  KEY idx_aud_login_log_user_id_created_at (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志';
