package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.github.keel.iam.application.authorization.RoleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新角色请求。{@code code} 不可修改。
 */
public record UpdateRoleRequest(
        @NotBlank @Size(max = 64) String name,
        @Schema(nullable = true) @Size(max = 255) String description,
        @NotNull RoleStatus status,
        @NotNull @Min(0) @Max(1_000_000) Integer sort,
        @NotNull @Min(0) Integer version) {
}
