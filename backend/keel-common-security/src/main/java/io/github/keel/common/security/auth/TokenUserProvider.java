package io.github.keel.common.security.auth;

import java.util.Optional;

/**
 * 认证过滤器加载用户的端口，由 IAM 模块实现。
 */
public interface TokenUserProvider {

    Optional<AuthenticatedUser> findActiveUser(long userId);
}
