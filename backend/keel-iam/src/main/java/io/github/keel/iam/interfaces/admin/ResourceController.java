package io.github.keel.iam.interfaces.admin;

import io.github.keel.common.core.result.Result;
import io.github.keel.iam.application.authorization.ResourceApplicationService;
import io.github.keel.iam.application.authorization.ResourceDetailView;
import io.github.keel.iam.application.authorization.ResourceNodeView;
import io.github.keel.iam.application.authorization.ResourceStatus;
import io.github.keel.iam.application.authorization.ResourceType;
import io.github.keel.iam.application.authorization.UpdateResourceCommand;
import io.github.keel.iam.interfaces.admin.dto.ResourceDetail;
import io.github.keel.iam.interfaces.admin.dto.ResourceNode;
import io.github.keel.iam.interfaces.admin.dto.UpdateResourceRequest;
import io.github.keel.iam.interfaces.admin.dto.UpdateResourceStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端资源树与展示属性维护接口。
 */
@Tag(name = "Resource", description = "管理端资源树与展示属性")
@RestController
@RequestMapping("/api/v1/admin/iam/resources")
public class ResourceController {

    private final ResourceApplicationService resourceApplicationService;

    public ResourceController(ResourceApplicationService resourceApplicationService) {
        this.resourceApplicationService = resourceApplicationService;
    }

    @Operation(operationId = "getResourceTree", summary = "资源树", description = "资源树是有界集合，不分页；可按状态与类型筛选。")
    @GetMapping("/tree")
    @PreAuthorize("@perm.has('iam:resource:list')")
    public Result<List<ResourceNode>> tree(
            @Parameter(description = "资源状态") @RequestParam(required = false) ResourceStatus status,
            @Parameter(description = "资源类型") @RequestParam(required = false) ResourceType type) {
        return Result.success(resourceApplicationService.tree(status, type).stream()
                .map(this::toNode)
                .toList());
    }

    @Operation(operationId = "getResource", summary = "资源详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('iam:resource:list')")
    public Result<ResourceDetail> detail(@PathVariable long id) {
        return Result.success(toDetail(resourceApplicationService.detail(id)));
    }

    @Operation(operationId = "updateResource", summary = "更新资源展示属性",
            description = "type、code、path、component 不可修改；父级只能是根、DIRECTORY 或 MENU，且不能形成环。")
    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('iam:resource:update')")
    public Result<ResourceDetail> update(@PathVariable long id,
                                         @Valid @RequestBody UpdateResourceRequest request) {
        ResourceDetailView view = resourceApplicationService.update(id, new UpdateResourceCommand(
                request.name(),
                request.parentId(),
                request.sort(),
                request.icon(),
                request.visible(),
                request.version()));
        return Result.success(toDetail(view));
    }

    @Operation(operationId = "updateResourceStatus", summary = "更新资源状态",
            description = "停用目录/菜单前校验启用子资源；状态变更后清理受影响用户权限缓存。")
    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('iam:resource:status')")
    public Result<ResourceDetail> updateStatus(@PathVariable long id,
                                               @Valid @RequestBody UpdateResourceStatusRequest request) {
        return Result.success(toDetail(resourceApplicationService.updateStatus(
                id, request.status(), request.version())));
    }

    private ResourceNode toNode(ResourceNodeView view) {
        return new ResourceNode(
                view.id(),
                view.parentId(),
                view.type(),
                view.code(),
                view.name(),
                view.path(),
                view.component(),
                view.icon(),
                view.sort(),
                view.visible(),
                view.status(),
                view.children().stream().map(this::toNode).toList());
    }

    private ResourceDetail toDetail(ResourceDetailView view) {
        return new ResourceDetail(
                view.id(),
                view.parentId(),
                view.type(),
                view.code(),
                view.name(),
                view.path(),
                view.component(),
                view.icon(),
                view.sort(),
                view.visible(),
                view.status(),
                view.createdAt(),
                view.updatedAt(),
                view.version());
    }
}
