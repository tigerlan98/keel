package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 登录响应。Web 模式通过 Cookie 传递 token，因此 {@code token} 为空。
 */
public record LoginResponse(
        @Schema(requiredMode = REQUIRED) UserBrief user,
        @Schema(requiredMode = REQUIRED) boolean superAdmin,
        @Schema(requiredMode = REQUIRED) List<String> permissions,
        @Schema(requiredMode = REQUIRED) List<MenuNode> menus,
        @Schema(requiredMode = REQUIRED) boolean mustChangePassword,
        String token) {
}
