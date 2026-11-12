package io.github.keel.iam.application.identity;

import java.util.List;

/**
 * 创建用户命令。
 */
public record CreateUserCommand(String username, String password, String nickname,
                                String email, String phone, List<Long> roleIds) {
}
