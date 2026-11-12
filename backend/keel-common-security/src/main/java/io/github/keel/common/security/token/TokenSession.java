package io.github.keel.common.security.token;

/**
 * 从 Redis 解析出的 token 会话元数据。
 */
public record TokenSession(long userId, String loginType, String device, String tokenHash) {
}
