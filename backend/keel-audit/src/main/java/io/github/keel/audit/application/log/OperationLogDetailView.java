package io.github.keel.audit.application.log;

import java.time.LocalDateTime;

/**
 * 操作日志详情。
 */
public record OperationLogDetailView(Long id, String traceId, Long userId, String username,
                                     String module, String action, String method,
                                     String requestMethod, String requestUri, String requestParams,
                                     Boolean success, String errorMessage, String ip, String userAgent,
                                     Integer durationMs, LocalDateTime createdAt) {
}
