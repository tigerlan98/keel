package io.github.keel.common.security.token;

import java.time.Duration;

/**
 * 新签发的 token 与空闲超时时间。
 */
public record IssuedToken(String token, Duration idleTimeout) {
}
