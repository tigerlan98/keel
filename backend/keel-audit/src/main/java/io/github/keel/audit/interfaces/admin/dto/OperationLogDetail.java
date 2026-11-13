package io.github.keel.audit.interfaces.admin.dto;

import java.time.LocalDateTime;

/**
 * 操作日志详情响应。
 */
public record OperationLogDetail(Long id, String traceId, Long userId, String username,
                                 String module, String action, String method,
                                 String requestMethod, String requestUri, String requestParams,
                                 Boolean success, String errorMessage, String ip, String userAgent,
                                 Integer durationMs, LocalDateTime createdAt) {
}
