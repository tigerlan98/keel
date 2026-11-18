package io.github.keel.iam.application.identity;

import java.time.LocalDateTime;

/** 用户列表行视图。 */
public record UserSummaryView(
        Long id,
        String username,
        String nickname,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        String phone,
        Long departmentId,
        String departmentName,
        java.util.List<io.github.keel.iam.application.authorization.RoleBriefView> roles) {}
