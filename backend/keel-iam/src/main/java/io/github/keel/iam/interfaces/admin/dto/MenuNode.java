package io.github.keel.iam.interfaces.admin.dto;

import java.util.List;

/**
 * 菜单树节点响应。
 */
public record MenuNode(Long id, Long parentId, String type, String code, String name, String path,
                       String component, String icon, Integer sort, Boolean visible,
                       List<MenuNode> children) {
}
