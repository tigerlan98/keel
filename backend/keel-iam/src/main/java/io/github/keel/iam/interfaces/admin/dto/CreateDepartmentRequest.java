package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.organization.DepartmentStatus;

import jakarta.validation.constraints.*;

public record CreateDepartmentRequest(
        @NotNull @Min(1) Long parentId,
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_-]{1,63}") String code,
        @NotBlank @Size(max = 64) String name,
        @NotNull @Min(0) @Max(9999) Integer sort,
        @NotNull DepartmentStatus status) {
    public CreateDepartmentRequest {
        name = name == null ? null : name.trim();
        code = code == null ? null : code.trim();
    }
}
