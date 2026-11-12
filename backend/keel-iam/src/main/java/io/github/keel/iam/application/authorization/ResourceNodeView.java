package io.github.keel.iam.application.authorization;

import java.util.List;

/**
 * 资源树节点视图。
 */
public record ResourceNodeView(Long id, Long parentId, ResourceType type, String code, String name,
                               String path, String component, String icon, Integer sort,
                               Boolean visible, ResourceStatus status, List<ResourceNodeView> children) {
}
