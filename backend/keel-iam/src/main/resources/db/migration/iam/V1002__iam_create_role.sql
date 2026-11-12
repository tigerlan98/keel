SET NAMES utf8mb4;

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
