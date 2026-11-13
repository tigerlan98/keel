package io.github.keel.audit.application.log;

import java.time.LocalDateTime;

/**
 * 操作日志摘要。
 */
public record OperationLogSummaryView(Long id, String traceId, Long userId, String username,
                                      String module, String action, String requestMethod,
                                      String requestUri, Boolean success, String ip,
                                      Integer durationMs, LocalDateTime createdAt) {
}
