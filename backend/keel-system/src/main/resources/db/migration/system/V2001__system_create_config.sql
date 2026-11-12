SET NAMES utf8mb4;

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
