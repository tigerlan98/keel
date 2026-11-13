package io.github.keel.audit.interfaces.admin.dto;

import java.time.LocalDateTime;

/**
 * 操作日志摘要响应。
 */
public record OperationLogSummary(Long id, String traceId, Long userId, String username,
                                  String module, String action, String requestMethod,
                                  String requestUri, Boolean success, String ip,
                                  Integer durationMs, LocalDateTime createdAt) {
}
