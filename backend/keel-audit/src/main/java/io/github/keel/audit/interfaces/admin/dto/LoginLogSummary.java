package io.github.keel.audit.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 登录日志摘要响应。
 */
public record LoginLogSummary(
        @Schema(requiredMode = REQUIRED) Long id,
        String traceId,
        Long userId,
        @Schema(requiredMode = REQUIRED) String username,
        @Schema(requiredMode = REQUIRED) String action,
        @Schema(requiredMode = REQUIRED) String loginType,
        @Schema(requiredMode = REQUIRED) String device,
        String ip,
        @Schema(requiredMode = REQUIRED) Boolean success,
        String failureReason,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt) {
}
