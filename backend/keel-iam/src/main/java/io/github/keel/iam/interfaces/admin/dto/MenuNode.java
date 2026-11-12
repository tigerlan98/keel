package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 菜单树节点响应。
 */
public record MenuNode(
        @Schema(requiredMode = REQUIRED) Long id,
        @Schema(requiredMode = REQUIRED) Long parentId,
        @Schema(requiredMode = REQUIRED) String type,
        @Schema(requiredMode = REQUIRED) String code,
        @Schema(requiredMode = REQUIRED) String name,
        String path,
        String component,
        String icon,
        @Schema(requiredMode = REQUIRED) Integer sort,
        @Schema(requiredMode = REQUIRED) Boolean visible,
        @Schema(requiredMode = REQUIRED) List<MenuNode> children) {
}
