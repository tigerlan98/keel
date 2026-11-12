SET NAMES utf8mb4;

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
