package io.github.keel.audit.interfaces.admin.dto;

import java.time.LocalDateTime;

/**
 * 登录日志详情响应。
 */
public record LoginLogDetail(Long id, String traceId, Long userId, String username,
                             String action, String loginType, String device, String ip,
                             String userAgent, Boolean success, String failureReason,
                             LocalDateTime createdAt) {
}
