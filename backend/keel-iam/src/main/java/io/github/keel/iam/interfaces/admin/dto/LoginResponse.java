package io.github.keel.iam.interfaces.admin.dto;

import java.util.List;

/**
 * 登录响应。Web 模式通过 Cookie 传递 token，因此 {@code token} 为空。
 */
public record LoginResponse(UserBrief user, boolean superAdmin, List<String> permissions,
                            List<MenuNode> menus, boolean mustChangePassword, String token) {
}
