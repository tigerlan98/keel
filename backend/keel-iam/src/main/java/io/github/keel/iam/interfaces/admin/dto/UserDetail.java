package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.identity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 用户详情响应。
 */
public record UserDetail(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) String username,
        String nickname,
        String email,
        String phone,
        @Schema(requiredMode = REQUIRED) UserStatus status,
        @Schema(requiredMode = REQUIRED) boolean mustChangePassword,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        @Schema(requiredMode = REQUIRED) List<RoleBrief> roles,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = REQUIRED) LocalDateTime updatedAt,
        @Schema(requiredMode = REQUIRED) Integer version) {
}
