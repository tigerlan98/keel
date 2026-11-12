package io.github.keel.iam.application.authorization;

/**
 * 角色摘要视图。
 */
public record RoleBriefView(Long id, String code, String name, RoleStatus status) {
}
