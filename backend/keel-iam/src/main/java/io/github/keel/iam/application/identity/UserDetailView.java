package io.github.keel.iam.application.identity;

import io.github.keel.iam.application.authorization.RoleBriefView;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情视图。
 */
public record UserDetailView(Long id, String username, String nickname, String email, String phone,
                             UserStatus status, boolean mustChangePassword,
                             LocalDateTime lastLoginAt, String lastLoginIp,
                             List<RoleBriefView> roles, LocalDateTime createdAt,
                             LocalDateTime updatedAt, Integer version) {
}
