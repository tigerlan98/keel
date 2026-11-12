package io.github.keel.iam.interfaces.admin.dto;

import java.time.LocalDateTime;

/**
 * 用户列表行响应。
 */
public record UserSummary(Long id, String username, String nickname, String status,
                          LocalDateTime lastLoginAt, LocalDateTime createdAt) {
}
