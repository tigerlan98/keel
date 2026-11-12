package io.github.keel.common.security.token;

import io.github.keel.common.security.config.SecurityTokenProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 不透明 token 的签发、校验与吊销。Redis 只保存 SHA-256 哈希。
 */
@Service
public class TokenService {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";
    private static final String USER_INDEX_PREFIX = "auth:user:";
    private static final String LOGIN_TYPE_ADMIN = "ADMIN";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final SecurityTokenProperties properties;

    public TokenService(StringRedisTemplate redis, SecurityTokenProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public IssuedToken issue(long userId, String loginType, String device, String ip, String userAgent) {
        boolean web = LOGIN_TYPE_ADMIN.equals(loginType);
        Duration idleTimeout = web ? properties.webIdleTimeout() : properties.appIdleTimeout();
        Duration absoluteTimeout = web ? properties.webAbsoluteTimeout() : properties.appAbsoluteTimeout();

        String rawToken = generateToken();
        String tokenHash = sha256(rawToken);
        String indexKey = indexKey(loginType, userId);

        // 同设备单会话：新登录踢掉该设备旧 token
        Object previousHash = redis.opsForHash().get(indexKey, device);
        if (previousHash != null) {
            redis.delete(tokenKey(previousHash.toString()));
        }

        long now = System.currentTimeMillis();
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("userId", Long.toString(userId));
        metadata.put("loginType", loginType);
        metadata.put("device", device);
        metadata.put("issuedAt", Long.toString(now));
        metadata.put("lastAccessAt", Long.toString(now));
        metadata.put("absoluteExpireAt", Long.toString(now + absoluteTimeout.toMillis()));
        if (ip != null && !ip.isBlank()) {
            metadata.put("ip", ip);
        }
        if (userAgent != null && !userAgent.isBlank()) {
            metadata.put("userAgent", userAgent);
        }

        String tokenKey = tokenKey(tokenHash);
        redis.opsForHash().putAll(tokenKey, metadata);
        redis.expire(tokenKey, idleTimeout);
        redis.opsForHash().put(indexKey, device, tokenHash);

        return new IssuedToken(rawToken, idleTimeout);
    }

    public Optional<TokenSession> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = sha256(rawToken);
        String tokenKey = tokenKey(tokenHash);
        Map<Object, Object> metadata = redis.opsForHash().entries(tokenKey);
        if (metadata.isEmpty()) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        long absoluteExpireAt = parseLong(metadata.get("absoluteExpireAt"), -1);
        if (absoluteExpireAt <= now) {
            revokeMetadata(tokenHash, metadata);
            return Optional.empty();
        }

        String loginType = value(metadata.get("loginType"));
        String device = value(metadata.get("device"));
        long userId = parseLong(metadata.get("userId"), -1);
        if (loginType == null || device == null || userId < 0) {
            redis.delete(tokenKey);
            return Optional.empty();
        }

        long lastAccessAt = parseLong(metadata.get("lastAccessAt"), 0);
        if (now - lastAccessAt >= properties.lastAccessWriteInterval().toMillis()) {
            redis.opsForHash().put(tokenKey, "lastAccessAt", Long.toString(now));
            redis.expire(tokenKey, idleTimeout(loginType));
        }

        return Optional.of(new TokenSession(userId, loginType, device, tokenHash));
    }

    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = sha256(rawToken);
        Map<Object, Object> metadata = redis.opsForHash().entries(tokenKey(tokenHash));
        if (!metadata.isEmpty()) {
            revokeMetadata(tokenHash, metadata);
        }
    }

    private void revokeMetadata(String tokenHash, Map<Object, Object> metadata) {
        redis.delete(tokenKey(tokenHash));
        String loginType = value(metadata.get("loginType"));
        String device = value(metadata.get("device"));
        long userId = parseLong(metadata.get("userId"), -1);
        if (loginType != null && device != null && userId >= 0) {
            redis.opsForHash().delete(indexKey(loginType, userId), device);
        }
    }

    private Duration idleTimeout(String loginType) {
        return LOGIN_TYPE_ADMIN.equals(loginType) ? properties.webIdleTimeout() : properties.appIdleTimeout();
    }

    private static String indexKey(String loginType, long userId) {
        return USER_INDEX_PREFIX + loginType + ":" + userId;
    }

    private static String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private static String value(Object value) {
        return value == null ? null : value.toString();
    }

    private static long parseLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
