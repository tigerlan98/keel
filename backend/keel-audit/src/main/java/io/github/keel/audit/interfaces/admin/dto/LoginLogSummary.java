package io.github.keel.audit.interfaces.admin.dto;

import java.time.LocalDateTime;

/**
 * 登录日志摘要响应。
 */
public record LoginLogSummary(Long id, String traceId, Long userId, String username,
                              String action, String loginType, String device, String ip,
                              Boolean success, String failureReason, LocalDateTime createdAt) {
}
