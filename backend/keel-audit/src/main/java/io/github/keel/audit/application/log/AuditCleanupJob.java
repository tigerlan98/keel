package io.github.keel.audit.application.log;

import io.github.keel.audit.infrastructure.persistence.mapper.LoginLogMapper;
import io.github.keel.audit.infrastructure.persistence.mapper.OperationLogMapper;
import io.github.keel.common.core.config.ConfigProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计日志保留期清理。每日 02:00 执行，每批 1000 条，避免长事务。
 */
@Component
@RequiredArgsConstructor
public class AuditCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AuditCleanupJob.class);
    private static final int BATCH_SIZE = 1000;
    private static final String OPERATION_RETENTION_KEY = "audit.operation.retention-days";
    private static final String LOGIN_RETENTION_KEY = "audit.login.retention-days";
    private static final int DEFAULT_OPERATION_RETENTION_DAYS = 180;
    private static final int DEFAULT_LOGIN_RETENTION_DAYS = 365;

    private final OperationLogMapper operationLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final ConfigProvider configProvider;

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanup() {
        cleanup(LocalDateTime.now());
    }

    /**
     * 按保留期分批删除日志，测试可直接传入基准时间。
     */
    public void cleanup(LocalDateTime now) {
        int operationDeleted = deleteInBatches(
                now.minusDays(retentionDays(OPERATION_RETENTION_KEY, DEFAULT_OPERATION_RETENTION_DAYS)),
                operationLogMapper::deleteBefore);
        int loginDeleted = deleteInBatches(
                now.minusDays(retentionDays(LOGIN_RETENTION_KEY, DEFAULT_LOGIN_RETENTION_DAYS)),
                loginLogMapper::deleteBefore);

        if (operationDeleted > 0 || loginDeleted > 0) {
            log.info("审计日志清理完成: operation={}, login={}", operationDeleted, loginDeleted);
        }
    }

    private int deleteInBatches(LocalDateTime cutoff, BatchDeleter deleter) {
        int total = 0;
        while (true) {
            int deleted = deleter.delete(cutoff, BATCH_SIZE);
            total += deleted;
            if (deleted < BATCH_SIZE) {
                return total;
            }
        }
    }

    private int retentionDays(String key, int defaultValue) {
        return configProvider.get(key)
                .map(value -> {
                    try {
                        int days = Integer.parseInt(value);
                        return days > 0 ? days : defaultValue;
                    } catch (NumberFormatException exception) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @FunctionalInterface
    private interface BatchDeleter {

        int delete(LocalDateTime cutoff, int limit);
    }
}
