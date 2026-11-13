package io.github.keel.system.interfaces.admin.dto;

import java.time.LocalDateTime;

/**
 * 系统参数摘要响应。
 */
public record ConfigSummary(Long id, String configKey, String configValue, String configGroup,
                            Boolean publicFlag, String description, LocalDateTime updatedAt,
                            Integer version) {
}
