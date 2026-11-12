package io.github.keel.iam.application.authorization;

import java.time.LocalDateTime;

/**
 * 角色详情视图。
 */
public record RoleDetailView(Long id, String code, String name, String description,
                             RoleStatus status, Integer sort, LocalDateTime createdAt,
                             LocalDateTime updatedAt, Integer version) {
}
