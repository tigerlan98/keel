SET NAMES utf8mb4;

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
