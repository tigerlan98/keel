package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.identity.UserStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 更新用户状态请求。
 */
public record UpdateUserStatusRequest(
        @NotNull UserStatus status,
        @NotNull @Min(0) Integer version) {
}
