package io.github.keel.common.core.auth;

/**
 * 当前登录用户端口。未登录时返回 {@code null}。
 *
 * <p>实现位于 common-security，供持久层审计字段与业务模块使用。</p>
 */
public interface CurrentUserProvider {

    Long currentUserId();
}
