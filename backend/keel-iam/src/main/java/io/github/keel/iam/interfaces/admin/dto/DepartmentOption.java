package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.organization.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record DepartmentOption(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long parentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DepartmentStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean selectable,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                java.util.List<DepartmentOption> children) {}
