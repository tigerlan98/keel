package io.github.keel.audit.interfaces.admin;

import io.github.keel.audit.application.log.LoginLogApplicationService;
import io.github.keel.audit.application.log.LoginLogDetailView;
import io.github.keel.audit.application.log.LoginLogPageCommand;
import io.github.keel.audit.application.log.LoginLogSummaryView;
import io.github.keel.audit.interfaces.admin.dto.LoginLogDetail;
import io.github.keel.audit.interfaces.admin.dto.LoginLogSummary;
import io.github.keel.common.core.event.LoginAction;
import io.github.keel.common.core.result.PageResult;
import io.github.keel.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 管理端登录日志查询接口，只读。
 */
@Tag(name = "LoginLog", description = "管理端登录日志")
@RestController
@RequestMapping("/api/v1/admin/audit/login-logs")
@Validated
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogApplicationService loginLogApplicationService;

    @Operation(operationId = "listLoginLogs", summary = "登录日志分页查询", description = "仅支持按写入时间排序。")
    @GetMapping
    @PreAuthorize("@perm.has('audit:login:list')")
    public Result<PageResult<LoginLogSummary>> list(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "排序，仅支持 createdAt")
            @RequestParam(required = false) @Size(max = 64) String sort,
            @Parameter(description = "开始时间，本地 ISO 时间")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间，本地 ISO 时间")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "用户 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "登录名前缀") @RequestParam(required = false) @Size(max = 64) String username,
            @Parameter(description = "动作：LOGIN / LOGOUT / LOGIN_FAILED") @RequestParam(required = false) LoginAction action,
            @Parameter(description = "是否成功") @RequestParam(required = false) Boolean success,
            @Parameter(description = "来源 IP") @RequestParam(required = false) @Size(max = 64) String ip) {
        PageResult<LoginLogSummaryView> result = loginLogApplicationService.page(
                new LoginLogPageCommand(page, size, sort, startTime, endTime,
                        userId, username, action, success, ip));
        return Result.success(new PageResult<>(
                result.items().stream().map(this::toSummary).toList(),
                result.total(), result.page(), result.size()));
    }

    @Operation(operationId = "getLoginLog", summary = "登录日志详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('audit:login:list')")
    public Result<LoginLogDetail> detail(@PathVariable long id) {
        return Result.success(toDetail(loginLogApplicationService.detail(id)));
    }

    private LoginLogSummary toSummary(LoginLogSummaryView view) {
        return new LoginLogSummary(
                view.id(),
                view.traceId(),
                view.userId(),
                view.username(),
                view.action(),
                view.loginType(),
                view.device(),
                view.ip(),
                view.success(),
                view.failureReason(),
                view.createdAt());
    }

    private LoginLogDetail toDetail(LoginLogDetailView view) {
        return new LoginLogDetail(
                view.id(),
                view.traceId(),
                view.userId(),
                view.username(),
                view.action(),
                view.loginType(),
                view.device(),
                view.ip(),
                view.userAgent(),
                view.success(),
                view.failureReason(),
                view.createdAt());
    }
}
