package io.github.keel.iam.application.authentication;

import io.github.keel.iam.application.authorization.MenuView;

import java.time.Duration;
import java.util.List;

/**
 * 登录或当前用户会话结果。
 */
public record AuthSession(AuthUserView user, boolean superAdmin, List<String> permissions,
                          List<MenuView> menus, boolean mustChangePassword,
                          String token, Duration idleTimeout, Device device) {
}
