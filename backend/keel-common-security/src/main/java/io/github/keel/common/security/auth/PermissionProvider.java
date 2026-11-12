package io.github.keel.common.security.auth;

import java.util.Set;

/**
 * 权限码与菜单的查询端口，由 IAM 模块实现。
 */
public interface PermissionProvider {

    /**
     * 用户被授予的权限码集合。超级管理员不依赖此集合。
     */
    Set<String> permissionsOf(long userId);
}
