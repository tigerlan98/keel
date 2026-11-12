package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户资料请求。username 不可修改；null 表示清空 email / phone。
 */
public record UpdateUserRequest(
        @NotBlank @Size(max = 64) String nickname,
        @Schema(nullable = true) @Email @Size(max = 128) String email,
        @Schema(nullable = true) @Pattern(regexp = "^[0-9+-]{6,32}$", message = "手机号格式不正确") String phone,
        @NotNull @Min(0) Integer version) {
}
