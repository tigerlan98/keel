package io.github.keel.system.interfaces.publicapi;

import io.github.keel.common.core.result.Result;
import io.github.keel.system.application.config.ConfigApplicationService;
import io.github.keel.system.application.config.PublicSiteConfigView;
import io.github.keel.system.interfaces.publicapi.dto.PublicSiteConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户公开接口：只返回 public_flag = 1 且属于 system.* 的参数。
 */
@Tag(name = "PublicSystem", description = "门户公开站点配置")
@RestController
@RequestMapping("/api/v1/public/system")
@RequiredArgsConstructor
public class PublicConfigController {

    private final ConfigApplicationService configApplicationService;

    @Operation(operationId = "getPublicSiteConfig", summary = "获取站点公开配置", description = "仅返回系统名称与 Logo 地址。")
    @ApiResponse(responseCode = "200", description = "站点配置")
    @SecurityRequirements
    @GetMapping("/config")
    public Result<PublicSiteConfig> config() {
        PublicSiteConfigView view = configApplicationService.publicSiteConfig();
        return Result.success(new PublicSiteConfig(view.systemName(), view.logoUrl()));
    }
}
