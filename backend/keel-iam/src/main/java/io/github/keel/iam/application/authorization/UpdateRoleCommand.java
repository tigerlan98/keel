package io.github.keel.iam.application.authorization;

/**
 * 更新角色命令。
 */
public record UpdateRoleCommand(String name, String description, RoleStatus status,
                                Integer sort, Integer version) {
}
