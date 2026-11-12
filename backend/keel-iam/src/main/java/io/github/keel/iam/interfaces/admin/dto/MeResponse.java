package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 当前用户响应。
 */
public record MeResponse(
        @Schema(requiredMode = REQUIRED) UserBrief user,
        @Schema(requiredMode = REQUIRED) boolean superAdmin,
        @Schema(requiredMode = REQUIRED) List<String> permissions,
        @Schema(requiredMode = REQUIRED) List<MenuNode> menus,
        @Schema(requiredMode = REQUIRED) boolean mustChangePassword) {
}
