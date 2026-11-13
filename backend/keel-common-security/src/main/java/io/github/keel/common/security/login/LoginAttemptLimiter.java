package io.github.keel.common.security.login;

import io.github.keel.common.core.config.ConfigProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * 登录失败计数与锁定。
 *
 * <p>按“用户名 + 来源 IP”计数，连续失败达到上限后锁定一段时间；计数只存 Redis，不落库。
 * 用户名统一转为小写，避免利用大小写变体绕过计数。</p>
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    private static final String KEY_PREFIX = "auth:login-fail:";
    private static final String MAX_FAILURES_KEY = "auth.max-login-failures";
    private static final String LOCK_MINUTES_KEY = "auth.lock-minutes";
    private static final int DEFAULT_MAX_FAILURES = 5;
    private static final int DEFAULT_LOCK_MINUTES = 15;

    private final StringRedisTemplate redis;
    private final ObjectProvider<ConfigProvider> configProvider;

    public boolean isLocked(String username, String ip) {
        String value = redis.opsForValue().get(key(username, ip));
        if (value == null) {
            return false;
        }
        try {
            return Long.parseLong(value) >= maxFailures();
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public void recordFailure(String username, String ip) {
        String key = key(username, ip);
        Long failures = redis.opsForValue().increment(key);
        if (failures == null) {
            return;
        }
        // 首次失败开始计时；达到上限时重新计时，确保完整锁定一段时间。
        if (failures == 1L || failures >= maxFailures()) {
            redis.expire(key, Duration.ofMinutes(lockMinutes()));
        }
    }

    public void reset(String username, String ip) {
        redis.delete(key(username, ip));
    }

    private int maxFailures() {
        return positiveInt(MAX_FAILURES_KEY, DEFAULT_MAX_FAILURES);
    }

    private int lockMinutes() {
        return positiveInt(LOCK_MINUTES_KEY, DEFAULT_LOCK_MINUTES);
    }

    private int positiveInt(String key, int defaultValue) {
        ConfigProvider provider = configProvider.getIfAvailable();
        if (provider == null) {
            return defaultValue;
        }
        return provider.get(key)
                .map(value -> {
                    try {
                        int parsed = Integer.parseInt(value.trim());
                        return parsed > 0 ? parsed : defaultValue;
                    } catch (NumberFormatException exception) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private static String key(String username, String ip) {
        String account = username == null || username.isBlank()
                ? "unknown"
                : username.trim().toLowerCase(Locale.ROOT);
        String source = ip == null || ip.isBlank() ? "unknown" : ip.trim();
        return KEY_PREFIX + account + ":" + source;
    }
}
