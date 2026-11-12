package io.github.keel.common.security.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * App / Mini 客户端登录不经过浏览器 Cookie，因此跳过 CSRF；Web 登录仍强制 CSRF。
 */
@Component
public class AppDeviceLoginRequestMatcher implements RequestMatcher {

    private static final String LOGIN_PATH = "/api/v1/admin/auth/login";

    private final ObjectMapper objectMapper;

    public AppDeviceLoginRequestMatcher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !LOGIN_PATH.equals(request.getRequestURI())) {
            return false;
        }
        try {
            String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(body);
            JsonNode device = root.get("device");
            if (device == null || !device.isTextual()) {
                return false;
            }
            String value = device.asText();
            return "APP".equals(value) || "MINI".equals(value);
        } catch (Exception exception) {
            return false;
        }
    }
}
