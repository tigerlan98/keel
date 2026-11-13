package io.github.keel.system.application.config;

import java.time.LocalDateTime;

/**
 * 系统参数详情。
 */
public record ConfigDetailView(Long id, String configKey, String configValue, String configGroup,
                               Boolean publicFlag, String description, LocalDateTime createdAt,
                               Long createdBy, LocalDateTime updatedAt, Long updatedBy,
                               Integer version) {
}
