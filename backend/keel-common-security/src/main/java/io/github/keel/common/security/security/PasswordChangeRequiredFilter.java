package io.github.keel.common.security.security;

import io.github.keel.common.security.auth.AuthenticatedUser;
import io.github.keel.common.security.error.AuthErrorCode;
import io.github.keel.common.security.web.SecurityErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 必须修改密码的用户只允许访问改密、登出、/me 与 CSRF。
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/admin/auth/me",
            "/api/v1/admin/auth/change-password",
            "/api/v1/admin/auth/logout",
            "/api/v1/admin/auth/csrf");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && user.mustChangePassword()
                && !ALLOWED_PATHS.contains(request.getRequestURI())) {
            SecurityErrorWriter.write(response, AuthErrorCode.AUTH_PASSWORD_CHANGE_REQUIRED);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
