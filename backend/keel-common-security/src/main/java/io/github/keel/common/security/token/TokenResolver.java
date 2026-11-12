package io.github.keel.common.security.token;

import io.github.keel.common.security.config.SecurityTokenProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 按 Authorization 头 → Cookie 的顺序解析 token。
 */
@Component
@RequiredArgsConstructor
public class TokenResolver {

    private final SecurityTokenProperties properties;

    public Optional<String> resolve(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String bearerToken = authorization.substring(7).trim();
            if (!bearerToken.isEmpty()) {
                return Optional.of(bearerToken);
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (properties.cookieName().equals(cookie.getName())) {
                    String cookieToken = cookie.getValue();
                    if (cookieToken != null && !cookieToken.isBlank()) {
                        return Optional.of(cookieToken);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
