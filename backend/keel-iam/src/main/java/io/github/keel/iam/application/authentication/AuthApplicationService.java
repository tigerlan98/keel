package io.github.keel.iam.application.authentication;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.keel.common.core.event.LoginAction;
import io.github.keel.common.core.event.LoginLogEvent;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.security.auth.AuthenticatedUser;
import io.github.keel.common.security.error.AuthErrorCode;
import io.github.keel.common.security.token.IssuedToken;
import io.github.keel.common.security.token.TokenService;
import io.github.keel.common.security.token.TokenSession;
import io.github.keel.iam.application.authorization.AuthorizationCacheEvictor;
import io.github.keel.iam.application.authorization.AuthorizationQueryService;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 登录、当前用户、改密与登出用例。
 */
@Service
public class AuthApplicationService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final IamUserMapper userMapper;
    private final AuthorizationQueryService authorizationQueryService;
    private final AuthorizationCacheEvictor authorizationCacheEvictor;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthApplicationService(IamUserMapper userMapper,
                                  AuthorizationQueryService authorizationQueryService,
                                  AuthorizationCacheEvictor authorizationCacheEvictor,
                                  PasswordEncoder passwordEncoder,
                                  TokenService tokenService,
                                  ApplicationEventPublisher eventPublisher) {
        this.userMapper = userMapper;
        this.authorizationQueryService = authorizationQueryService;
        this.authorizationCacheEvictor = authorizationCacheEvictor;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.eventPublisher = eventPublisher;
    }

    public AuthSession login(LoginCommand command) {
        IamUserPO user = userMapper.selectOne(Wrappers.<IamUserPO>lambdaQuery()
                .eq(IamUserPO::getUsername, command.username()));

        if (user == null) {
            publishLoginLog(null, command.username(), command, LoginAction.LOGIN_FAILED, false, "BAD_CREDENTIALS");
            throw new BusinessException(AuthErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            publishLoginLog(user.getId(), user.getUsername(), command, LoginAction.LOGIN_FAILED, false, "DISABLED");
            throw new BusinessException(AuthErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            publishLoginLog(user.getId(), user.getUsername(), command, LoginAction.LOGIN_FAILED, false, "BAD_CREDENTIALS");
            throw new BusinessException(AuthErrorCode.AUTH_BAD_CREDENTIALS);
        }

        boolean superAdmin = userMapper.countSuperAdminRole(user.getId()) > 0;
        IssuedToken issuedToken = tokenService.issue(
                user.getId(),
                command.device().loginType(),
                command.device().name(),
                command.ip(),
                command.userAgent());

        publishLoginLog(user.getId(), user.getUsername(), command, LoginAction.LOGIN, true, null);

        return new AuthSession(
                toUserView(user),
                superAdmin,
                authorizationQueryService.permissions(user.getId(), superAdmin),
                authorizationQueryService.menus(user.getId(), superAdmin),
                Boolean.TRUE.equals(user.getMustChangePassword()),
                issuedToken.token(),
                issuedToken.idleTimeout(),
                command.device());
    }

    public AuthSession currentSession(AuthenticatedUser principal) {
        IamUserPO user = userMapper.selectById(principal.id());
        if (user == null || !STATUS_ENABLED.equals(user.getStatus())) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID);
        }

        return new AuthSession(
                toUserView(user),
                principal.superAdmin(),
                authorizationQueryService.permissions(user.getId(), principal.superAdmin()),
                authorizationQueryService.menus(user.getId(), principal.superAdmin()),
                Boolean.TRUE.equals(user.getMustChangePassword()),
                null,
                null,
                null);
    }

    /**
     * 修改自己的密码：校验旧密码，更新后吊销全部 token 并清理权限缓存。
     */
    public void changePassword(AuthenticatedUser principal, String oldPassword, String newPassword) {
        IamUserPO user = userMapper.selectById(principal.id());
        if (user == null || !STATUS_ENABLED.equals(user.getStatus())) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.AUTH_OLD_PASSWORD_INVALID);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userMapper.updateById(user);

        tokenService.revokeAll(user.getId());
        authorizationCacheEvictor.evictUser(user.getId());
    }

    public void logout(String rawToken, AuthenticatedUser principal, String ip, String userAgent) {
        Optional<TokenSession> session = tokenService.authenticate(rawToken);
        tokenService.revoke(rawToken);
        session.ifPresent(tokenSession -> eventPublisher.publishEvent(new LoginLogEvent(
                principal.id(),
                principal.username(),
                LoginAction.LOGOUT,
                tokenSession.loginType(),
                tokenSession.device(),
                ip,
                userAgent,
                true,
                null)));
    }

    private void publishLoginLog(Long userId, String username, LoginCommand command,
                                 LoginAction action, boolean success, String failureReason) {
        eventPublisher.publishEvent(new LoginLogEvent(
                userId,
                username,
                action,
                command.device().loginType(),
                command.device().name(),
                command.ip(),
                command.userAgent(),
                success,
                failureReason));
    }

    private AuthUserView toUserView(IamUserPO user) {
        return new AuthUserView(user.getId(), user.getUsername(), user.getNickname());
    }
}
