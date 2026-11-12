package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.authorization.RoleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建角色请求。
 */
public record CreateRoleRequest(
        @NotBlank @Size(min = 2, max = 64)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色编码必须为大写字母、数字和下划线")
        String code,
        @NotBlank @Size(max = 64) String name,
        @Size(max = 255) String description,
        RoleStatus status,
        @Min(0) @Max(1_000_000) Integer sort) {
}
