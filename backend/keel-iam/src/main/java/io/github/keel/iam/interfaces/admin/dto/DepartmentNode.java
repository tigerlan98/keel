package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.organization.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record DepartmentNode(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long parentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer sort,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DepartmentStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long directUserCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                java.util.List<DepartmentNode> children) {}
