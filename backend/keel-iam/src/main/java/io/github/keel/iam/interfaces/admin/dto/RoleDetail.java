package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.authorization.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 角色详情响应。
 */
public record RoleDetail(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) String code,
        @Schema(requiredMode = REQUIRED) String name,
        String description,
        @Schema(requiredMode = REQUIRED) RoleStatus status,
        @Schema(requiredMode = REQUIRED) Integer sort,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = REQUIRED) LocalDateTime updatedAt,
        @Schema(requiredMode = REQUIRED) Integer version) {
}
