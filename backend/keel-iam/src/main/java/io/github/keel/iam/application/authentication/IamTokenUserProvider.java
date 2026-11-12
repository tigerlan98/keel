package io.github.keel.iam.application.authentication;

import io.github.keel.common.security.auth.AuthenticatedUser;
import io.github.keel.common.security.auth.TokenUserProvider;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 认证过滤器加载 IAM 用户的端口实现。
 */
@Component
@RequiredArgsConstructor
public class IamTokenUserProvider implements TokenUserProvider {

    private static final String STATUS_ENABLED = "ENABLED";

    private final IamUserMapper userMapper;

    @Override
    public Optional<AuthenticatedUser> findActiveUser(long userId) {
        IamUserPO user = userMapper.selectById(userId);
        if (user == null || !STATUS_ENABLED.equals(user.getStatus())) {
            return Optional.empty();
        }
        boolean superAdmin = userMapper.countSuperAdminRole(userId) > 0;
        return Optional.of(new AuthenticatedUser(
                user.getId(), user.getUsername(), user.getNickname(), superAdmin,
                Boolean.TRUE.equals(user.getMustChangePassword())));
    }
}
