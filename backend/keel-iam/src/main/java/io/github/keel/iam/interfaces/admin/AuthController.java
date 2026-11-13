package io.github.keel.iam.interfaces.admin;

import io.github.keel.common.core.audit.OperationLog;
import io.github.keel.common.core.result.Result;
import io.github.keel.common.security.auth.AuthenticatedUser;
import io.github.keel.common.security.config.SecurityTokenProperties;
import io.github.keel.common.security.token.TokenResolver;
import io.github.keel.iam.application.authentication.AuthApplicationService;
import io.github.keel.iam.application.authentication.AuthSession;
import io.github.keel.iam.application.authentication.AuthUserView;
import io.github.keel.iam.application.authentication.Device;
import io.github.keel.iam.application.authentication.LoginCommand;
import io.github.keel.iam.application.authorization.MenuView;
import io.github.keel.iam.interfaces.admin.dto.ChangePasswordRequest;
import io.github.keel.iam.interfaces.admin.dto.CsrfResponse;
import io.github.keel.iam.interfaces.admin.dto.LoginRequest;
import io.github.keel.iam.interfaces.admin.dto.LoginResponse;
import io.github.keel.iam.interfaces.admin.dto.MeResponse;
import io.github.keel.iam.interfaces.admin.dto.MenuNode;
import io.github.keel.iam.interfaces.admin.dto.UserBrief;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "Auth", description = "管理端认证与会话")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final TokenResolver tokenResolver;
    private final SecurityTokenProperties securityTokenProperties;

    @Operation(operationId = "login", summary = "登录",
            description = "Web 模式写入 HttpOnly Cookie；App / MINI 模式在响应体中返回 token。同一设备再次登录会使旧 token 失效。")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
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

    @Operation(operationId = "logout", summary = "登出", description = "吊销当前 token 并清除 Cookie；会话已失效时同样返回 204。")
    @ApiResponse(responseCode = "204", description = "已登出")
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

    @Operation(operationId = "me", summary = "当前用户会话", description = "返回用户、权限码、菜单树与是否必须修改密码。")
    @ApiResponse(responseCode = "200", description = "当前会话")
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

    @Operation(operationId = "changePassword", summary = "修改自己的密码", description = "校验旧密码；成功后吊销该用户全部 token，客户端需重新登录。")
    @ApiResponse(responseCode = "204", description = "密码已修改")
    @PostMapping("/change-password")
    @OperationLog(module = "iam", action = "UPDATE")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authApplicationService.changePassword(principal, request.oldPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "csrf", summary = "获取 CSRF token",
            description = "同时写入 XSRF-TOKEN Cookie；登录与写请求必须通过 X-XSRF-TOKEN 请求头回传。")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "CSRF token")
    @GetMapping("/csrf")
    public Result<CsrfResponse> csrf(@Parameter(hidden = true) CsrfToken csrfToken) {
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
