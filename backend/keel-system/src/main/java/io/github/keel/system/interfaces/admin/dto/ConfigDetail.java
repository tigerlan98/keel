package io.github.keel.system.interfaces.admin.dto;

import java.time.LocalDateTime;

/**
 * 系统参数详情响应。
 */
public record ConfigDetail(Long id, String configKey, String configValue, String configGroup,
                           Boolean publicFlag, String description, LocalDateTime createdAt,
                           Long createdBy, LocalDateTime updatedAt, Long updatedBy,
                           Integer version) {
}
