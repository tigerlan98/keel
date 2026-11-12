package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 角色已授权资源。
 */
public record RoleResourcesResponse(@Schema(requiredMode = REQUIRED) List<Long> resourceIds) {
}
