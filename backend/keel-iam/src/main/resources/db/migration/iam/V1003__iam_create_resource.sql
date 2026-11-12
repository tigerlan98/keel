SET NAMES utf8mb4;

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
