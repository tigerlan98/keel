package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.authorization.ResourceStatus;
import io.github.keel.iam.application.authorization.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 资源详情响应。
 */
public record ResourceDetail(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) Long parentId,
        @Schema(requiredMode = REQUIRED) ResourceType type,
        @Schema(requiredMode = REQUIRED) String code,
        @Schema(requiredMode = REQUIRED) String name,
        String path,
        String component,
        String icon,
        @Schema(requiredMode = REQUIRED) Integer sort,
        @Schema(requiredMode = REQUIRED) Boolean visible,
        @Schema(requiredMode = REQUIRED) ResourceStatus status,
        @Schema(requiredMode = REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = REQUIRED) LocalDateTime updatedAt,
        @Schema(requiredMode = REQUIRED) Integer version) {
}
