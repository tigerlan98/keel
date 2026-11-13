package io.github.keel.audit.application.log;

import java.time.LocalDateTime;

/**
 * 登录日志摘要。
 */
public record LoginLogSummaryView(Long id, String traceId, Long userId, String username,
                                  String action, String loginType, String device, String ip,
                                  Boolean success, String failureReason, LocalDateTime createdAt) {
}
