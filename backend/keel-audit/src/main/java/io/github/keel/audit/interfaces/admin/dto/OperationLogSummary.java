package io.github.keel.audit.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 操作日志摘要响应。
 */
public record OperationLogSummary(
        @Schema(requiredMode = REQUIRED) Long id,
        String traceId,
        Long userId,
        String username,
        @Schema(requiredMode = REQUIRED) String module,
        @Schema(requiredMode = REQUIRED) String action,
        String requestMethod,
        String requestUri,
        @Schema(requiredMode = REQUIRED) Boolean success,
        String ip,
        Integer durationMs,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt) {
}
