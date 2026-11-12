package io.github.keel.iam.interfaces.admin.dto;

import io.github.keel.iam.application.authorization.ResourceStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 更新资源状态请求。
 */
public record UpdateResourceStatusRequest(
        @NotNull ResourceStatus status,
        @NotNull @Min(0) Integer version) {
}
