package io.github.keel.common.persistence.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 统一填充审计字段。
 *
 * <p>当前尚无登录上下文，操作人统一记为系统用户 0；接入
 * {@code CurrentUserProvider} 后改为真实用户 ID。</p>
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final Long SYSTEM_USER_ID = 0L;

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, SYSTEM_USER_ID);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedBy", Long.class, SYSTEM_USER_ID);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updatedBy", Long.class, SYSTEM_USER_ID);
    }
}
