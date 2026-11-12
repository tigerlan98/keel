package io.github.keel.common.security.config;

import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.error.ErrorCode;
import io.github.keel.common.security.auth.TokenUserProvider;
import io.github.keel.common.security.error.AuthErrorCode;
import io.github.keel.common.security.security.PasswordChangeRequiredFilter;
import io.github.keel.common.security.security.TokenAuthenticationFilter;
import io.github.keel.common.security.token.TokenResolver;
import io.github.keel.common.security.token.TokenService;
import io.github.keel.common.security.web.AppDeviceLoginRequestMatcher;
import io.github.keel.common.security.web.SecurityErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;

/**
 * 无状态安全配置：opaque token、CSRF 双模式、401/403 统一响应。
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityTokenProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TokenService tokenService,
                                                   TokenResolver tokenResolver,
                                                   TokenUserProvider tokenUserProvider,
                                                   AppDeviceLoginRequestMatcher appDeviceLoginRequestMatcher)
            throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                SecurityConfig::isBearerRequest,
                                appDeviceLoginRequestMatcher,
                                request -> request.getRequestURI().startsWith("/api/v1/public/")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/auth/login", "/api/v1/admin/auth/csrf").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, CommonErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> {
                            if (exception instanceof CsrfException) {
                                writeError(response, AuthErrorCode.AUTH_CSRF_INVALID);
                            } else {
                                writeError(response, CommonErrorCode.FORBIDDEN);
                            }
                        }))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(new TokenAuthenticationFilter(tokenResolver, tokenService, tokenUserProvider),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new PasswordChangeRequiredFilter(), TokenAuthenticationFilter.class);

        return http.build();
    }

    private static boolean isBearerRequest(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
    }

    private static void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        SecurityErrorWriter.write(response, errorCode);
    }
}
