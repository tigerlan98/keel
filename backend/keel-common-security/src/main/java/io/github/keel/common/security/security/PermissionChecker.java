package io.github.keel.common.security.security;

import io.github.keel.common.security.auth.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 权限判断入口，供 {@code @PreAuthorize("@perm.has('...')")} 使用。
 *
 * <p>Phase 1A 只区分超级管理员；权限码集合在 RBAC 阶段接入。</p>
 */
@Component("perm")
public class PermissionChecker {

    public boolean has(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return user.superAdmin();
    }
}
