SET NAMES utf8mb4;

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
