package io.github.keel.iam.interfaces.admin;

import io.github.keel.common.core.result.Result;
import io.github.keel.common.security.auth.AuthenticatedUser;
import io.github.keel.common.security.config.SecurityTokenProperties;
import io.github.keel.common.security.token.TokenResolver;
import io.github.keel.iam.application.authentication.AuthApplicationService;
import io.github.keel.iam.application.authentication.AuthSession;
import io.github.keel.iam.application.authentication.AuthUserView;
import io.github.keel.iam.application.authentication.Device;
import io.github.keel.iam.application.authentication.LoginCommand;
import io.github.keel.iam.application.authentication.MenuView;
import io.github.keel.iam.interfaces.admin.dto.CsrfResponse;
import io.github.keel.iam.interfaces.admin.dto.LoginRequest;
import io.github.keel.iam.interfaces.admin.dto.LoginResponse;
import io.github.keel.iam.interfaces.admin.dto.MeResponse;
import io.github.keel.iam.interfaces.admin.dto.MenuNode;
import io.github.keel.iam.interfaces.admin.dto.UserBrief;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端认证接口。
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final TokenResolver tokenResolver;
    private final SecurityTokenProperties securityTokenProperties;

    public AuthController(AuthApplicationService authApplicationService,
                          TokenResolver tokenResolver,
                          SecurityTokenProperties securityTokenProperties) {
        this.authApplicationService = authApplicationService;
        this.tokenResolver = tokenResolver;
        this.securityTokenProperties = securityTokenProperties;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        LoginCommand command = new LoginCommand(
                request.username(),
                request.password(),
                request.device(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader(HttpHeaders.USER_AGENT));

        AuthSession session = authApplicationService.login(command);

        if (session.device() == Device.WEB) {
            ResponseCookie cookie = ResponseCookie.from(securityTokenProperties.cookieName(), session.token())
                    .httpOnly(true)
                    .secure(securityTokenProperties.cookieSecure())
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(session.idleTimeout())
                    .build();
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        String token = session.device() == Device.WEB ? null : session.token();
        return Result.success(new LoginResponse(
                toUserBrief(session.user()),
                session.superAdmin(),
                session.permissions(),
                toMenuNodes(session.menus()),
                session.mustChangePassword(),
                token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response,
                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal != null) {
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
            tokenResolver.resolve(request).ifPresent(token ->
                    authApplicationService.logout(token, principal, ip, userAgent));
        }

        ResponseCookie cookie = ResponseCookie.from(securityTokenProperties.cookieName(), "")
                .httpOnly(true)
                .secure(securityTokenProperties.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Result<MeResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        AuthSession session = authApplicationService.currentSession(principal);
        return Result.success(new MeResponse(
                toUserBrief(session.user()),
                session.superAdmin(),
                session.permissions(),
                toMenuNodes(session.menus()),
                session.mustChangePassword()));
    }

    @GetMapping("/csrf")
    public Result<CsrfResponse> csrf(CsrfToken csrfToken) {
        return Result.success(new CsrfResponse(csrfToken.getToken()));
    }

    private UserBrief toUserBrief(AuthUserView user) {
        return new UserBrief(user.id(), user.username(), user.nickname());
    }

    private List<MenuNode> toMenuNodes(List<MenuView> menus) {
        return menus.stream().map(this::toMenuNode).toList();
    }

    private MenuNode toMenuNode(MenuView menu) {
        return new MenuNode(
                menu.id(),
                menu.parentId(),
                menu.type(),
                menu.code(),
                menu.name(),
                menu.path(),
                menu.component(),
                menu.icon(),
                menu.sort(),
                menu.visible(),
                toMenuNodes(menu.children()));
    }
}
