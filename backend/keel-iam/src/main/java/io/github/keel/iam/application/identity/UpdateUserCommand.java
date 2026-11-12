package io.github.keel.iam.application.identity;

/**
 * 更新用户资料命令。
 */
public record UpdateUserCommand(String nickname, String email, String phone, Integer version) {
}
