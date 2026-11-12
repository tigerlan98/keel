package io.github.keel.iam.application.authorization;

/**
 * 角色分页查询条件。
 */
public record RolePageCommand(Integer page, Integer size, String sort, String keyword, RoleStatus status) {
}
