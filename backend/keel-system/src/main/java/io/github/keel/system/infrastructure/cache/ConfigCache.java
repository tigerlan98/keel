package io.github.keel.system.infrastructure.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 系统参数值缓存：{@code sys:config:{key}}，TTL 30 分钟。
 */
@Component
public class ConfigCache {

    private static final String KEY_PREFIX = "sys:config:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;

    public ConfigCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get(cacheKey(key)));
    }

    public void put(String key, String value) {
        redis.opsForValue().set(cacheKey(key), value, TTL);
    }

    public void evict(String key) {
        redis.delete(cacheKey(key));
    }

    private static String cacheKey(String key) {
        return KEY_PREFIX + key;
    }
}
