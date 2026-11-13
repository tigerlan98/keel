package io.github.keel.system.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 系统参数摘要响应。
 */
public record ConfigSummary(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) String configKey,
        @Schema(requiredMode = REQUIRED) String configValue,
        @Schema(requiredMode = REQUIRED) String configGroup,
        @Schema(requiredMode = REQUIRED) Boolean publicFlag,
        String description,
        @Schema(requiredMode = REQUIRED) LocalDateTime updatedAt,
        @Schema(requiredMode = REQUIRED) Integer version) {
}
