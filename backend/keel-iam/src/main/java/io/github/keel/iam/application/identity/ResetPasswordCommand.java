package io.github.keel.iam.application.identity;

/**
 * 管理员重置用户密码命令。
 */
public record ResetPasswordCommand(String newPassword, Integer version) {
}
