package io.github.keel.common.security.security;

import io.github.keel.common.security.auth.TokenUserProvider;
import io.github.keel.common.security.token.TokenResolver;
import io.github.keel.common.security.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 opaque token 并写入 SecurityContext。
 */
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_ATTRIBUTE = "keel.auth.token";

    private final TokenResolver tokenResolver;
    private final TokenService tokenService;
    private final TokenUserProvider tokenUserProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenResolver.resolve(request).ifPresent(rawToken -> tokenService.authenticate(rawToken)
                    .flatMap(session -> tokenUserProvider.findActiveUser(session.userId()))
                    .ifPresent(user -> {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, List.of());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        request.setAttribute(TOKEN_ATTRIBUTE, rawToken);
                    }));
        }
        filterChain.doFilter(request, response);
    }
}
