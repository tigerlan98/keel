package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.authorization.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 角色摘要。
 */
public record RoleBrief(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) String code,
        @Schema(requiredMode = REQUIRED) String name,
        @Schema(requiredMode = REQUIRED) RoleStatus status) {
}
