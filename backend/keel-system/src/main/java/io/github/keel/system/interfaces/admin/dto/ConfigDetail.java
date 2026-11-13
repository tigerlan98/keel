package io.github.keel.system.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 系统参数详情响应。
 */
public record ConfigDetail(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) String configKey,
        @Schema(requiredMode = REQUIRED) String configValue,
        @Schema(requiredMode = REQUIRED) String configGroup,
        @Schema(requiredMode = REQUIRED) Boolean publicFlag,
        String description,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = REQUIRED) Long createdBy,
        @Schema(requiredMode = REQUIRED) LocalDateTime updatedAt,
        @Schema(requiredMode = REQUIRED) Long updatedBy,
        @Schema(requiredMode = REQUIRED) Integer version) {
}
