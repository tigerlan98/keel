package io.github.keel.audit.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 操作日志详情响应。
 */
public record OperationLogDetail(
        @Schema(requiredMode = REQUIRED) Long id,
        String traceId,
        Long userId,
        String username,
        @Schema(requiredMode = REQUIRED) String module,
        @Schema(requiredMode = REQUIRED) String action,
        String method,
        String requestMethod,
        String requestUri,
        String requestParams,
        @Schema(requiredMode = REQUIRED) Boolean success,
        String errorMessage,
        String ip,
        String userAgent,
        Integer durationMs,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt) {
}
