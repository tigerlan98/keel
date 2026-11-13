package io.github.keel.system.application.config;

import java.time.LocalDateTime;

/**
 * 系统参数摘要。
 */
public record ConfigSummaryView(Long id, String configKey, String configValue, String configGroup,
                                Boolean publicFlag, String description, LocalDateTime updatedAt,
                                Integer version) {
}
