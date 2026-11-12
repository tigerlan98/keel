package io.github.keel.common.security.auth;

/**
 * 已认证用户上下文。
 */
public record AuthenticatedUser(long id, String username, String nickname, boolean superAdmin,
                               boolean mustChangePassword) {
}
