package io.github.keel.iam.interfaces.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 全量替换角色资源。
 */
public record AssignResourcesRequest(
        @NotNull @Size(max = 1000) List<Long> resourceIds) {
}
