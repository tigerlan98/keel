package io.github.keel.iam.application.identity;

/**
 * 用户分页查询条件。
 */
public record UserPageCommand(Integer page, Integer size, String sort, String username, UserStatus status) {
}
