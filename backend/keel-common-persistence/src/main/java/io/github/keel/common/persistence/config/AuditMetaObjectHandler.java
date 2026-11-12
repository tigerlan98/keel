package io.github.keel.common.persistence.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.github.keel.common.core.auth.CurrentUserProvider;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 统一填充审计字段，操作人来自当前登录用户；未登录时记为系统用户 0。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final Long SYSTEM_USER_ID = 0L;

    private final ObjectProvider<CurrentUserProvider> currentUserProvider;

    public AuditMetaObjectHandler(ObjectProvider<CurrentUserProvider> currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = currentUserId();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, operatorId);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedBy", Long.class, operatorId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId());
    }

    private Long currentUserId() {
        CurrentUserProvider provider = currentUserProvider.getIfAvailable();
        Long userId = provider == null ? null : provider.currentUserId();
        return userId == null ? SYSTEM_USER_ID : userId;
    }
}
