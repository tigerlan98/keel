package io.github.keel.iam.interfaces.admin;

import io.github.keel.common.core.result.PageResult;
import io.github.keel.common.core.result.Result;
import io.github.keel.iam.application.authentication.Device;
import io.github.keel.iam.application.authorization.RoleBriefView;
import io.github.keel.iam.application.identity.CreateUserCommand;
import io.github.keel.iam.application.identity.ResetPasswordCommand;
import io.github.keel.iam.application.identity.UpdateUserCommand;
import io.github.keel.iam.application.identity.UserApplicationService;
import io.github.keel.iam.application.identity.UserDetailView;
import io.github.keel.iam.application.identity.UserPageCommand;
import io.github.keel.iam.application.identity.UserQueryApplicationService;
import io.github.keel.iam.application.identity.UserStatus;
import io.github.keel.iam.application.identity.UserSummaryView;
import io.github.keel.iam.interfaces.admin.dto.AssignRolesRequest;
import io.github.keel.iam.interfaces.admin.dto.CreateUserRequest;
import io.github.keel.iam.interfaces.admin.dto.ResetPasswordRequest;
import io.github.keel.iam.interfaces.admin.dto.RoleBrief;
import io.github.keel.iam.interfaces.admin.dto.UpdateUserRequest;
import io.github.keel.iam.interfaces.admin.dto.UpdateUserStatusRequest;
import io.github.keel.iam.interfaces.admin.dto.UserDetail;
import io.github.keel.iam.interfaces.admin.dto.UserRolesResponse;
import io.github.keel.iam.interfaces.admin.dto.UserSummary;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户查询与维护接口。
 */
@Tag(name = "User", description = "管理端用户查询与维护")
@RestController
@RequestMapping("/api/v1/admin/iam/users")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserQueryApplicationService userQueryApplicationService;
    private final UserApplicationService userApplicationService;

    @Operation(operationId = "listUsers", summary = "用户分页查询", description = "支持用户名前缀、状态筛选与白名单排序。")
    @GetMapping
    @PreAuthorize("@perm.has('iam:user:list')")
    public Result<PageResult<UserSummary>> list(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "排序，格式 field,asc|desc；字段白名单：username、createdAt、lastLoginAt")
            @RequestParam(required = false) @Size(max = 64) String sort,
            @Parameter(description = "用户名前缀") @RequestParam(required = false) @Size(max = 64) String username,
            @Parameter(description = "用户状态") @RequestParam(required = false) UserStatus status) {
        PageResult<UserSummaryView> result = userQueryApplicationService.page(
                new UserPageCommand(page, size, sort, username, status));

        return Result.success(new PageResult<>(
                result.items().stream().map(this::toSummary).toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    @Operation(operationId = "getUser", summary = "用户详情", description = "包含用户资料与已分配角色。")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('iam:user:list')")
    public Result<UserDetail> detail(@PathVariable long id) {
        return Result.success(toDetail(userApplicationService.detail(id)));
    }

    @Operation(operationId = "createUser", summary = "创建用户", description = "用户名唯一；只允许分配启用角色；初始密码要求首次登录修改。")
    @ApiResponse(responseCode = "201", description = "创建成功")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@perm.has('iam:user:create')")
    public Result<UserDetail> create(@Valid @RequestBody CreateUserRequest request) {
        UserDetailView view = userApplicationService.create(new CreateUserCommand(
                request.username(),
                request.password(),
                request.nickname(),
                request.email(),
                request.phone(),
                request.roleIds()));
        return Result.success(toDetail(view));
    }

    @Operation(operationId = "updateUser", summary = "更新用户资料", description = "username 不可修改；email / phone 传 null 表示清空。")
    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('iam:user:update')")
    public Result<UserDetail> update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request) {
        UserDetailView view = userApplicationService.update(id, new UpdateUserCommand(
                request.nickname(),
                request.email(),
                request.phone(),
                request.version()));
        return Result.success(toDetail(view));
    }

    @Operation(operationId = "updateUserStatus", summary = "启用/禁用用户", description = "禁用时立即吊销该用户全部 token 并清理权限缓存。")
    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.has('iam:user:status')")
    public Result<UserDetail> updateStatus(@PathVariable long id,
                                           @Valid @RequestBody UpdateUserStatusRequest request) {
        return Result.success(toDetail(userApplicationService.updateStatus(
                id, request.status(), request.version())));
    }

    @Operation(operationId = "resetUserPassword", summary = "重置用户密码", description = "设置首次登录改密并吊销全部 token。")
    @ApiResponse(responseCode = "204", description = "密码已重置")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("@perm.has('iam:user:reset-password')")
    public ResponseEntity<Void> resetPassword(@PathVariable long id,
                                              @Valid @RequestBody ResetPasswordRequest request) {
        userApplicationService.resetPassword(id, new ResetPasswordCommand(
                request.newPassword(),
                request.version()));
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "getUserRoles", summary = "查询用户角色")
    @GetMapping("/{id}/roles")
    @PreAuthorize("@perm.has('iam:user:list')")
    public Result<UserRolesResponse> roles(@PathVariable long id) {
        return Result.success(new UserRolesResponse(userApplicationService.rolesOf(id).stream()
                .map(this::toRoleBrief)
                .toList()));
    }

    @Operation(operationId = "replaceUserRoles", summary = "全量替换用户角色", description = "只允许启用角色；不使用 version，最后一次写入生效。")
    @ApiResponse(responseCode = "204", description = "分配成功")
    @PutMapping("/{id}/roles")
    @PreAuthorize("@perm.has('iam:user:assign-role')")
    public ResponseEntity<Void> replaceRoles(@PathVariable long id,
                                             @Valid @RequestBody AssignRolesRequest request) {
        userApplicationService.replaceRoles(id, request.roleIds());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "forceLogout", summary = "强制下线（全部设备）")
    @ApiResponse(responseCode = "204", description = "已下线")
    @DeleteMapping("/{id}/sessions")
    @PreAuthorize("@perm.has('iam:user:force-logout')")
    public ResponseEntity<Void> forceLogout(@PathVariable long id) {
        userApplicationService.forceLogout(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "forceLogoutDevice", summary = "强制下线（指定设备）", description = "无会话时同样返回 204。")
    @ApiResponse(responseCode = "204", description = "已下线")
    @DeleteMapping("/{id}/sessions/{device}")
    @PreAuthorize("@perm.has('iam:user:force-logout')")
    public ResponseEntity<Void> forceLogoutDevice(@PathVariable long id, @PathVariable Device device) {
        userApplicationService.forceLogoutDevice(id, device.loginType(), device.name());
        return ResponseEntity.noContent().build();
    }

    private UserSummary toSummary(UserSummaryView view) {
        return new UserSummary(
                view.id(),
                view.username(),
                view.nickname(),
                UserStatus.valueOf(view.status()),
                view.lastLoginAt(),
                view.createdAt());
    }

    private UserDetail toDetail(UserDetailView view) {
        return new UserDetail(
                view.id(),
                view.username(),
                view.nickname(),
                view.email(),
                view.phone(),
                view.status(),
                view.mustChangePassword(),
                view.lastLoginAt(),
                view.lastLoginIp(),
                view.roles().stream().map(this::toRoleBrief).toList(),
                view.createdAt(),
                view.updatedAt(),
                view.version());
    }

    private RoleBrief toRoleBrief(RoleBriefView view) {
        return new RoleBrief(view.id(), view.code(), view.name(), view.status());
    }
}
