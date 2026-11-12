package io.github.keel.iam.application.authorization;

import java.util.List;

/**
 * 当前用户可见菜单树节点。
 */
public record MenuView(Long id, Long parentId, String type, String code, String name, String path,
                       String component, String icon, Integer sort, Boolean visible,
                       List<MenuView> children) {
}
