package io.github.keel.iam.application.authorization;

import io.github.keel.iam.infrastructure.cache.PermissionCache;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import org.springframework.stereotype.Component;

/**
 * 权限缓存失效策略：用户变更精确失效，角色变更按引用用户失效，资源变更全部失效。
 */
@Component
public class AuthorizationCacheEvictor {

    private final PermissionCache permissionCache;
    private final IamUserMapper userMapper;

    public AuthorizationCacheEvictor(PermissionCache permissionCache, IamUserMapper userMapper) {
        this.permissionCache = permissionCache;
        this.userMapper = userMapper;
    }

    public void evictUser(long userId) {
        permissionCache.evict(userId);
    }

    public void evictRole(long roleId) {
        userMapper.selectUserIdsByRoleId(roleId).forEach(permissionCache::evict);
    }

    public void evictAll() {
        permissionCache.evictAll();
    }
}
