package io.github.keel.iam.application.authorization;

/**
 * 更新资源展示属性命令。
 */
public record UpdateResourceCommand(String name, Long parentId, Integer sort, String icon,
                                   Boolean visible, Integer version) {
}
