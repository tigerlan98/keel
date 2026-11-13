package io.github.keel.system.interfaces.publicapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 门户公开站点配置响应。
 */
public record PublicSiteConfig(
        @Schema(requiredMode = REQUIRED) String systemName,
        @Schema(requiredMode = REQUIRED) String logoUrl) {
}
