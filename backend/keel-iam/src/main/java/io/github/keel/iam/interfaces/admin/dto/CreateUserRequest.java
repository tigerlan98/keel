package io.github.keel.iam.interfaces.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建用户请求。
 */
public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "用户名只能包含字母、数字、下划线、点和连字符")
        String username,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password,
        @NotBlank @Size(max = 64) String nickname,
        @Email @Size(max = 128) String email,
        @Pattern(regexp = "^[0-9+-]{6,32}$", message = "手机号格式不正确") String phone,
        @Size(max = 100) List<Long> roleIds) {
}
