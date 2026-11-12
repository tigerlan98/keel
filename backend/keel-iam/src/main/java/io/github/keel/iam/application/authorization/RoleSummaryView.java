package io.github.keel.iam.application.authorization;

import java.time.LocalDateTime;

/**
 * 角色列表行视图。
 */
public record RoleSummaryView(Long id, String code, String name, String description,
                              RoleStatus status, Integer sort, LocalDateTime createdAt) {
}
