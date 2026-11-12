package io.github.keel.iam.interfaces.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 全量替换用户角色。
 */
public record AssignRolesRequest(
        @NotNull @Size(max = 100) List<Long> roleIds) {
}
