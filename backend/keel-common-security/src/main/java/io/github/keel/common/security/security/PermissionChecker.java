package io.github.keel.common.security.security;

import io.github.keel.common.security.auth.AuthenticatedUser;
import io.github.keel.common.security.auth.PermissionProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 权限判断入口，供 {@code @PreAuthorize("@perm.has('...')")} 使用。
 *
 * <p>{@code SUPER_ADMIN} 直接放行，其余用户按权限码集合判断。</p>
 */
@Component("perm")
@RequiredArgsConstructor
public class PermissionChecker {

    private final PermissionProvider permissionProvider;

    public boolean has(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        if (user.superAdmin()) {
            return true;
        }
        return permissionProvider.permissionsOf(user.id()).contains(permission);
    }
}
