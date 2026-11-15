package io.github.keel.iam.interfaces.admin;

import io.github.keel.common.core.audit.OperationLog;
import io.github.keel.common.core.result.Result;
import io.github.keel.iam.application.organization.*;
import io.github.keel.iam.interfaces.admin.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/iam/departments")
@Tag(name = "Department", description = "部门与组织归属管理")
@Validated
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentApplicationService service;

    @GetMapping("/tree")
    @Operation(
            operationId = "getDepartmentTree",
            summary = "部门树",
            description = "最多1000个节点的有界集合，包含停用部门，不分页。")
    @PreAuthorize("@perm.has('iam:department:list')")
    public Result<List<DepartmentNode>> tree() {
        return Result.success(service.tree().stream().map(this::node).toList());
    }

    @GetMapping("/options")
    @Operation(
            operationId = "getDepartmentOptions",
            summary = "部门筛选与选择选项",
            description = "有界树，只提供归属选择需要的最小字段；停用项仍可用于用户筛选。")
    @PreAuthorize(
            "@perm.has('iam:department:list') or @perm.has('iam:department:create') or"
                + " @perm.has('iam:department:update') or @perm.has('iam:user:list') or"
                + " @perm.has('iam:user:create') or @perm.has('iam:user:update')")
    public Result<List<DepartmentOption>> options() {
        return Result.success(service.options().stream().map(this::option).toList());
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getDepartment", summary = "部门详情")
    @PreAuthorize("@perm.has('iam:department:list')")
    public Result<DepartmentDetail> detail(@PathVariable @Min(1) long id) {
        return Result.success(detail(service.detail(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "部门已创建")
    @Operation(operationId = "createDepartment", summary = "新增部门")
    @PreAuthorize("@perm.has('iam:department:create')")
    @OperationLog(module = "iam", action = "CREATE")
    public Result<DepartmentDetail> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return Result.success(
                detail(
                        service.create(
                                new CreateDepartmentCommand(
                                        request.parentId(),
                                        request.code(),
                                        request.name(),
                                        request.sort(),
                                        request.status()))));
    }

    @PutMapping("/{id}")
    @Operation(
            operationId = "updateDepartment",
            summary = "更新部门资料",
            description = "编码不可修改；根不可移动，不能形成环。")
    @PreAuthorize("@perm.has('iam:department:update')")
    @OperationLog(module = "iam", action = "UPDATE")
    public Result<DepartmentDetail> update(
            @PathVariable @Min(1) long id, @Valid @RequestBody UpdateDepartmentRequest request) {
        return Result.success(
                detail(
                        service.update(
                                id,
                                new UpdateDepartmentCommand(
                                        request.parentId(),
                                        request.name(),
                                        request.sort(),
                                        request.version()))));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            operationId = "updateDepartmentStatus",
            summary = "启停部门",
            description = "根不可停用；存在启用后代时拒绝停用，不自动禁用用户。")
    @PreAuthorize("@perm.has('iam:department:status')")
    @OperationLog(module = "iam", action = "STATUS_CHANGE")
    public Result<DepartmentDetail> status(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody UpdateDepartmentStatusRequest request) {
        return Result.success(
                detail(service.changeStatus(id, request.status(), request.version())));
    }

    @DeleteMapping("/{id}")
    @Operation(
            operationId = "deleteDepartment",
            summary = "删除空叶部门",
            description = "存在子部门或关联用户时拒绝删除；version为必填非负数。")
    @ApiResponse(responseCode = "204", description = "部门已删除")
    @PreAuthorize("@perm.has('iam:department:delete')")
    @OperationLog(module = "iam", action = "DELETE")
    public ResponseEntity<Void> delete(
            @PathVariable @Min(1) long id, @RequestParam @Min(0) int version) {
        service.delete(id, version);
        return ResponseEntity.noContent().build();
    }

    private DepartmentDetail detail(DepartmentDetailView v) {
        return new DepartmentDetail(
                v.id(),
                v.parentId(),
                v.code(),
                v.name(),
                v.sort(),
                v.status(),
                v.version(),
                v.directUserCount(),
                v.createdAt(),
                v.updatedAt());
    }

    private DepartmentNode node(DepartmentNodeView v) {
        return new DepartmentNode(
                v.id(),
                v.parentId(),
                v.code(),
                v.name(),
                v.sort(),
                v.status(),
                v.version(),
                v.directUserCount(),
                v.children().stream().map(this::node).toList());
    }

    private DepartmentOption option(DepartmentOptionView v) {
        return new DepartmentOption(
                v.id(),
                v.parentId(),
                v.name(),
                v.status(),
                v.selectable(),
                v.children().stream().map(this::option).toList());
    }
}
