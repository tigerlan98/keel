package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.authentication.Device;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 登录请求。
 */
public record LoginRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 1, max = 72) String password,
        @NotNull Device device) {
}
