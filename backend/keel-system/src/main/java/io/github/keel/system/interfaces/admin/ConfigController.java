package io.github.keel.system.interfaces.admin;

import io.github.keel.common.core.audit.OperationLog;
import io.github.keel.common.core.result.PageResult;
import io.github.keel.common.core.result.Result;
import io.github.keel.system.application.config.ConfigApplicationService;
import io.github.keel.system.application.config.ConfigDetailView;
import io.github.keel.system.application.config.ConfigPageCommand;
import io.github.keel.system.application.config.ConfigSummaryView;
import io.github.keel.system.application.config.UpdateConfigCommand;
import io.github.keel.system.interfaces.admin.dto.ConfigDetail;
import io.github.keel.system.interfaces.admin.dto.ConfigSummary;
import io.github.keel.system.interfaces.admin.dto.UpdateConfigRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端系统参数接口。参数新增/删除仅由迁移维护，本接口只允许更新参数值。
 */
@Tag(name = "SystemConfig", description = "管理端系统参数")
@RestController
@RequestMapping("/api/v1/admin/system/configs")
@Validated
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigApplicationService configApplicationService;

    @Operation(operationId = "listConfigs", summary = "系统参数分页查询", description = "支持分组、key 前缀筛选与白名单排序。")
    @GetMapping
    @PreAuthorize("@perm.has('sys:config:list')")
    public Result<PageResult<ConfigSummary>> list(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "排序，格式 field,asc|desc；字段白名单：configKey、updatedAt")
            @RequestParam(required = false) @Size(max = 64) String sort,
            @Parameter(description = "参数分组") @RequestParam(required = false) @Size(max = 64) String group,
            @Parameter(description = "参数 key 前缀") @RequestParam(required = false) @Size(max = 128) String keyword) {
        PageResult<ConfigSummaryView> result = configApplicationService.page(
                new ConfigPageCommand(page, size, sort, group, keyword));
        return Result.success(new PageResult<>(
                result.items().stream().map(this::toSummary).toList(),
                result.total(), result.page(), result.size()));
    }

    @Operation(operationId = "getConfig", summary = "系统参数详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('sys:config:list')")
    public Result<ConfigDetail> detail(@PathVariable long id) {
        return Result.success(toDetail(configApplicationService.detail(id)));
    }

    @Operation(operationId = "updateConfig", summary = "更新系统参数值", description = "configKey 不可修改；公开参数仅允许 system.*；更新后立即清理参数缓存。")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('sys:config:update')")
    @OperationLog(module = "system", action = "UPDATE")
    public Result<ConfigDetail> update(@PathVariable long id, @Valid @RequestBody UpdateConfigRequest request) {
        ConfigDetailView view = configApplicationService.update(id, new UpdateConfigCommand(
                request.configValue(),
                request.description(),
                request.publicFlag(),
                request.version()));
        return Result.success(toDetail(view));
    }

    private ConfigSummary toSummary(ConfigSummaryView view) {
        return new ConfigSummary(
                view.id(),
                view.configKey(),
                view.configValue(),
                view.configGroup(),
                view.publicFlag(),
                view.description(),
                view.updatedAt(),
                view.version());
    }

    private ConfigDetail toDetail(ConfigDetailView view) {
        return new ConfigDetail(
                view.id(),
                view.configKey(),
                view.configValue(),
                view.configGroup(),
                view.publicFlag(),
                view.description(),
                view.createdAt(),
                view.createdBy(),
                view.updatedAt(),
                view.updatedBy(),
                view.version());
    }
}
