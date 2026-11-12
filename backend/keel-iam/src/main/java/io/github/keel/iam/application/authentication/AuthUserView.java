package io.github.keel.iam.application.authentication;

/**
 * 认证结果中的用户摘要。
 */
public record AuthUserView(long id, String username, String nickname) {
}
