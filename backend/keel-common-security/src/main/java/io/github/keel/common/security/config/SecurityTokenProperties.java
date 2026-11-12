package io.github.keel.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * 认证 token 与会话配置。
 */
@ConfigurationProperties(prefix = "keel.security")
public record SecurityTokenProperties(
        @DefaultValue("KEEL_TOKEN") String cookieName,
        @DefaultValue("false") boolean cookieSecure,
        @DefaultValue("30m") Duration webIdleTimeout,
        @DefaultValue("12h") Duration webAbsoluteTimeout,
        @DefaultValue("7d") Duration appIdleTimeout,
        @DefaultValue("30d") Duration appAbsoluteTimeout,
        @DefaultValue("5m") Duration lastAccessWriteInterval) {
}
