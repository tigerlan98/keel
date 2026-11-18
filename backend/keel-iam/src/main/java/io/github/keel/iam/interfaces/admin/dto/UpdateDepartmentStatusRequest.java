package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.organization.DepartmentStatus;

import jakarta.validation.constraints.*;

public record UpdateDepartmentStatusRequest(
        @NotNull DepartmentStatus status, @NotNull @Min(0) Integer version) {}
