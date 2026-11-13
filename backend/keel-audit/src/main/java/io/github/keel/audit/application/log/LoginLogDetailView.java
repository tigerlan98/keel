package io.github.keel.audit.application.log;

import java.time.LocalDateTime;

/**
 * 登录日志详情。
 */
public record LoginLogDetailView(Long id, String traceId, Long userId, String username,
                                 String action, String loginType, String device, String ip,
                                 String userAgent, Boolean success, String failureReason,
                                 LocalDateTime createdAt) {
}
