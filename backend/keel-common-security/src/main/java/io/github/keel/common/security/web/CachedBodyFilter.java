package io.github.keel.common.security.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 仅缓存登录请求体，供 CSRF 的 device 匹配器读取，同时保证 Controller 仍能读取。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CachedBodyFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/admin/auth/login";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request instanceof CachedBodyHttpServletRequest) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
        } catch (IOException exception) {
            throw new ServletException("读取登录请求体失败", exception);
        }
    }
}
