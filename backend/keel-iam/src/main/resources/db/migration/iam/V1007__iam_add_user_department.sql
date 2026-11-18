ALTER TABLE iam_user
  ADD COLUMN department_id BIGINT NULL COMMENT '所属部门ID，历史或兼容账号可为空' AFTER phone,
  ADD KEY idx_iam_user_department_id_created_at (department_id, created_at);
