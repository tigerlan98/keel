package io.github.keel.iam.infrastructure.cache;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 用户权限码缓存：{@code iam:perm:{userId}}，TTL 30 分钟。
 *
 * <p>权限码不含逗号，直接以逗号分隔存储空集合为 {@code ""}。</p>
 */
@Component
public class PermissionCache {

    private static final String KEY_PREFIX = "iam:perm:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;

    public PermissionCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<Set<String>> get(long userId) {
        String value = redis.opsForValue().get(key(userId));
        if (value == null) {
            return Optional.empty();
        }
        if (value.isEmpty()) {
            return Optional.of(Set.of());
        }
        return Optional.of(new LinkedHashSet<>(Arrays.asList(value.split(","))));
    }

    public void put(long userId, Set<String> permissions) {
        redis.opsForValue().set(key(userId), String.join(",", permissions), TTL);
    }

    public void evict(long userId) {
        redis.delete(key(userId));
    }

    /**
     * 资源变更可能影响任意用户的菜单，采用 SCAN 清理全部权限缓存。
     */
    public void evictAll() {
        ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(500).build();
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redis.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    private static String key(long userId) {
        return KEY_PREFIX + userId;
    }
}
