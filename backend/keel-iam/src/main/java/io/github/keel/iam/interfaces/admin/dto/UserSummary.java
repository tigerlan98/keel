package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.identity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 用户列表行响应。
 */
public record UserSummary(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) String username,
        String nickname,
        @Schema(requiredMode = REQUIRED) UserStatus status,
        LocalDateTime lastLoginAt,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt) {
}
