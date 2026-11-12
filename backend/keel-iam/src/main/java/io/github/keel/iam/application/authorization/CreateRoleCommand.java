package io.github.keel.iam.application.authorization;

/**
 * 创建角色命令。
 */
public record CreateRoleCommand(String code, String name, String description,
                                RoleStatus status, Integer sort) {
}
