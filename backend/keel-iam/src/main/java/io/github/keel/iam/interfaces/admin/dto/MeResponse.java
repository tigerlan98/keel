package io.github.keel.iam.interfaces.admin.dto;

import java.util.List;

/**
 * 当前用户响应。
 */
public record MeResponse(UserBrief user, boolean superAdmin, List<String> permissions,
                         List<MenuNode> menus, boolean mustChangePassword) {
}
