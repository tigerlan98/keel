package io.github.keel.iam.application.authorization;

import io.github.keel.common.security.auth.PermissionProvider;
import io.github.keel.iam.infrastructure.cache.PermissionCache;
import io.github.keel.iam.infrastructure.persistence.mapper.IamResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 权限码查询：优先读 Redis 缓存，未命中时按用户角色关联查询。
 */
@Component
@RequiredArgsConstructor
public class IamPermissionProvider implements PermissionProvider {

    private final IamResourceMapper resourceMapper;
    private final PermissionCache permissionCache;

    @Override
    public Set<String> permissionsOf(long userId) {
        return permissionCache.get(userId).orElseGet(() -> {
            Set<String> permissions = new LinkedHashSet<>(resourceMapper.selectPermissionCodes(userId));
            permissionCache.put(userId, permissions);
            return permissions;
        });
    }
}
