package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 用户已分配角色。
 */
public record UserRolesResponse(@Schema(requiredMode = REQUIRED) List<RoleBrief> roles) {
}
