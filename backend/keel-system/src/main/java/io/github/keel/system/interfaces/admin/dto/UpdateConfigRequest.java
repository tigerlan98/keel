package io.github.keel.system.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新系统参数请求。description 传 null 表示清空。
 */
public record UpdateConfigRequest(
        @NotNull @Size(max = 4000) String configValue,
        @Schema(nullable = true) @Size(max = 255) String description,
        @NotNull Boolean publicFlag,
        @NotNull Integer version) {
}
