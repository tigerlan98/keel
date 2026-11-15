package io.github.keel.iam.interfaces.admin.dto;

import jakarta.validation.constraints.*;

public record UpdateDepartmentRequest(
        @NotNull @Min(0) Long parentId,
        @NotBlank @Size(max = 64) String name,
        @NotNull @Min(0) @Max(9999) Integer sort,
        @NotNull @Min(0) Integer version) {
    public UpdateDepartmentRequest {
        name = name == null ? null : name.trim();
    }
}
