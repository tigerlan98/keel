package io.github.keel.iam.application.authentication;

/**
 * 登录用例入参。
 */
public record LoginCommand(String username, String password, Device device, String ip, String userAgent) {
}
