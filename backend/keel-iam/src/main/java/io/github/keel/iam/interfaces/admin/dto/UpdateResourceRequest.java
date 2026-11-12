package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新资源展示属性请求。type/code/path/component 由迁移维护。
 */
public record UpdateResourceRequest(
        @NotBlank @Size(max = 64) String name,
        @NotNull @Min(0) Long parentId,
        @NotNull @Min(0) @Max(1_000_000) Integer sort,
        @Schema(nullable = true) @Size(max = 64) String icon,
        @NotNull Boolean visible,
        @NotNull @Min(0) Integer version) {
}
