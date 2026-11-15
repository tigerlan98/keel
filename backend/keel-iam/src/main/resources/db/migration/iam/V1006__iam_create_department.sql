CREATE TABLE iam_department (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，仅内置根为0',
  code VARCHAR(64) NOT NULL COMMENT '部门编码，创建后不可修改',
  name VARCHAR(64) NOT NULL COMMENT '部门名称',
  sort INT NOT NULL DEFAULT 0 COMMENT '同级排序，升序',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  created_at DATETIME(3) NOT NULL COMMENT '创建时间',
  created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人ID，0=系统',
  updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
  updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人ID，0=系统',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_iam_department_code (code),
  UNIQUE KEY uk_iam_department_parent_id_name (parent_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IAM 部门';

INSERT INTO iam_department (parent_id, code, name, sort, status, created_at, created_by, updated_at, updated_by, version)
SELECT 0, 'ROOT', '总公司', 0, 'ENABLED', NOW(3), 0, NOW(3), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_department WHERE code = 'ROOT');
