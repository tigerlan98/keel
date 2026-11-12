package io.github.keel.iam.interfaces.admin;

import io.github.keel.common.core.result.PageResult;
import io.github.keel.common.core.result.Result;
import io.github.keel.iam.application.authorization.CreateRoleCommand;
import io.github.keel.iam.application.authorization.RoleApplicationService;
import io.github.keel.iam.application.authorization.RoleDetailView;
import io.github.keel.iam.application.authorization.RolePageCommand;
import io.github.keel.iam.application.authorization.RoleStatus;
import io.github.keel.iam.application.authorization.RoleSummaryView;
import io.github.keel.iam.application.authorization.UpdateRoleCommand;
import io.github.keel.iam.interfaces.admin.dto.AssignResourcesRequest;
import io.github.keel.iam.interfaces.admin.dto.CreateRoleRequest;
import io.github.keel.iam.interfaces.admin.dto.RoleDetail;
import io.github.keel.iam.interfaces.admin.dto.RoleResourcesResponse;
import io.github.keel.iam.interfaces.admin.dto.RoleSummary;
import io.github.keel.iam.interfaces.admin.dto.UpdateRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端角色与角色资源授权接口。
 */
@Tag(name = "Role", description = "管理端角色与资源授权")
@RestController
@RequestMapping("/api/v1/admin/iam/roles")
@Validated
@RequiredArgsConstructor
public class RoleController {

    private final RoleApplicationService roleApplicationService;

    @Operation(operationId = "listRoles", summary = "角色分页查询", description = "支持名称/编码前缀与状态筛选。")
    @GetMapping
    @PreAuthorize("@perm.has('iam:role:list')")
    public Result<PageResult<RoleSummary>> list(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "排序，白名单：sort、createdAt、code")
            @RequestParam(required = false) @Size(max = 64) String sort,
            @Parameter(description = "角色名或编码前缀") @RequestParam(required = false) @Size(max = 64) String keyword,
            @Parameter(description = "角色状态") @RequestParam(required = false) RoleStatus status) {
        PageResult<RoleSummaryView> result = roleApplicationService.page(
                new RolePageCommand(page, size, sort, keyword, status));
        return Result.success(new PageResult<>(
                result.items().stream().map(this::toSummary).toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    @Operation(operationId = "getRole", summary = "角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('iam:role:list')")
    public Result<RoleDetail> detail(@PathVariable long id) {
        return Result.success(toDetail(roleApplicationService.detail(id)));
    }

    @Operation(operationId = "createRole", summary = "创建角色", description = "角色编码创建后不可修改。")
    @ApiResponse(responseCode = "201", description = "创建成功")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perm.has('iam:role:create')")
    public Result<RoleDetail> create(@Valid @RequestBody CreateRoleRequest request) {
        RoleDetailView view = roleApplicationService.create(new CreateRoleCommand(
                request.code(),
                request.name(),
                request.description(),
                request.status(),
                request.sort()));
        return Result.success(toDetail(view));
    }

    @Operation(operationId = "updateRole", summary = "更新角色", description = "code 不可修改；SUPER_ADMIN 不允许禁用。状态变更后清理受影响用户权限缓存。")
    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('iam:role:update')")
    public Result<RoleDetail> update(@PathVariable long id, @Valid @RequestBody UpdateRoleRequest request) {
        RoleDetailView view = roleApplicationService.update(id, new UpdateRoleCommand(
                request.name(),
                request.description(),
                request.status(),
                request.sort(),
                request.version()));
        return Result.success(toDetail(view));
    }

    @Operation(operationId = "deleteRole", summary = "删除角色", description = "受保护角色或仍被用户引用的角色返回 409。")
    @ApiResponse(responseCode = "204", description = "删除成功")
    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('iam:role:delete')")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        roleApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "getRoleResources", summary = "查询角色已授权资源")
    @GetMapping("/{id}/resources")
    @PreAuthorize("@perm.has('iam:role:list')")
    public Result<RoleResourcesResponse> resources(@PathVariable long id) {
        return Result.success(new RoleResourcesResponse(roleApplicationService.resourceIds(id)));
    }

    @Operation(operationId = "replaceRoleResources", summary = "全量替换角色资源", description = "幂等全量替换，不使用 version；下一次请求即生效。")
    @ApiResponse(responseCode = "204", description = "授权成功")
    @PutMapping("/{id}/resources")
    @PreAuthorize("@perm.has('iam:role:assign-resource')")
    public ResponseEntity<Void> replaceResources(@PathVariable long id,
                                                 @Valid @RequestBody AssignResourcesRequest request) {
        roleApplicationService.replaceResources(id, request.resourceIds());
        return ResponseEntity.noContent().build();
    }

    private RoleSummary toSummary(RoleSummaryView view) {
        return new RoleSummary(
                view.id(),
                view.code(),
                view.name(),
                view.description(),
                view.status(),
                view.sort(),
                view.createdAt());
    }

    private RoleDetail toDetail(RoleDetailView view) {
        return new RoleDetail(
                view.id(),
                view.code(),
                view.name(),
                view.description(),
                view.status(),
                view.sort(),
                view.createdAt(),
                view.updatedAt(),
                view.version());
    }
}
