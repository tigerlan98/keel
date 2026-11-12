package io.github.keel.iam.application.authorization;

import java.time.LocalDateTime;

/**
 * 资源详情视图。
 */
public record ResourceDetailView(Long id, Long parentId, ResourceType type, String code, String name,
                                 String path, String component, String icon, Integer sort,
                                 Boolean visible, ResourceStatus status,
                                 LocalDateTime createdAt, LocalDateTime updatedAt, Integer version) {
}
