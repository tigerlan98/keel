package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.authorization.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 角色列表行响应。
 */
public record RoleSummary(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) String code,
        @Schema(requiredMode = REQUIRED) String name,
        String description,
        @Schema(requiredMode = REQUIRED) RoleStatus status,
        @Schema(requiredMode = REQUIRED) Integer sort,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt) {
}
