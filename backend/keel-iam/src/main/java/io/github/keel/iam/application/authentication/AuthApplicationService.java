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
import io.github.keel.iam.infrastructure.persistence.mapper.IamResourceMapper;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamResourcePO;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 登录、当前用户与登出用例。
 *
 * <p>Phase 1A 只加载超级管理员菜单；普通管理员权限码与菜单在 RBAC 阶段接入。</p>
 */
@Service
public class AuthApplicationService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final IamUserMapper userMapper;
    private final IamResourceMapper resourceMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthApplicationService(IamUserMapper userMapper,
                                  IamResourceMapper resourceMapper,
                                  PasswordEncoder passwordEncoder,
                                  TokenService tokenService,
                                  ApplicationEventPublisher eventPublisher) {
        this.userMapper = userMapper;
        this.resourceMapper = resourceMapper;
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
                List.of(),
                superAdmin ? loadEnabledMenus() : List.of(),
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
                List.of(),
                principal.superAdmin() ? loadEnabledMenus() : List.of(),
                Boolean.TRUE.equals(user.getMustChangePassword()),
                null,
                null,
                null);
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

    private List<MenuView> loadEnabledMenus() {
        List<IamResourcePO> resources = resourceMapper.selectList(Wrappers.<IamResourcePO>lambdaQuery()
                .in(IamResourcePO::getType, "DIRECTORY", "MENU")
                .eq(IamResourcePO::getStatus, STATUS_ENABLED)
                .eq(IamResourcePO::getVisible, true)
                .orderByAsc(IamResourcePO::getSort)
                .orderByAsc(IamResourcePO::getId));

        Map<Long, MenuView> nodes = new LinkedHashMap<>();
        for (IamResourcePO resource : resources) {
            nodes.put(resource.getId(), new MenuView(
                    resource.getId(),
                    resource.getParentId(),
                    resource.getType(),
                    resource.getCode(),
                    resource.getName(),
                    resource.getPath(),
                    resource.getComponent(),
                    resource.getIcon(),
                    resource.getSort(),
                    resource.getVisible(),
                    new ArrayList<>()));
        }

        List<MenuView> roots = new ArrayList<>();
        for (MenuView node : nodes.values()) {
            MenuView parent = node.parentId() == null ? null : nodes.get(node.parentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    private AuthUserView toUserView(IamUserPO user) {
        return new AuthUserView(user.getId(), user.getUsername(), user.getNickname());
    }
}
