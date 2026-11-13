package io.github.keel.audit.interfaces.admin;

import io.github.keel.audit.application.log.OperationLogApplicationService;
import io.github.keel.audit.application.log.OperationLogDetailView;
import io.github.keel.audit.application.log.OperationLogPageCommand;
import io.github.keel.audit.application.log.OperationLogSummaryView;
import io.github.keel.audit.interfaces.admin.dto.OperationLogDetail;
import io.github.keel.audit.interfaces.admin.dto.OperationLogSummary;
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
 * 管理端操作日志查询接口，只读。
 */
@Tag(name = "OperationLog", description = "管理端操作日志")
@RestController
@RequestMapping("/api/v1/admin/audit/operation-logs")
@Validated
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogApplicationService operationLogApplicationService;

    @Operation(operationId = "listOperationLogs", summary = "操作日志分页查询", description = "仅支持按写入时间排序。")
    @GetMapping
    @PreAuthorize("@perm.has('audit:operation:list')")
    public Result<PageResult<OperationLogSummary>> list(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "排序，仅支持 createdAt")
            @RequestParam(required = false) @Size(max = 64) String sort,
            @Parameter(description = "开始时间，本地 ISO 时间")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间，本地 ISO 时间")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "操作人 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "操作人用户名前缀") @RequestParam(required = false) @Size(max = 64) String username,
            @Parameter(description = "模块编码") @RequestParam(required = false) @Size(max = 32) String module,
            @Parameter(description = "动作编码") @RequestParam(required = false) @Size(max = 32) String action,
            @Parameter(description = "是否成功") @RequestParam(required = false) Boolean success) {
        PageResult<OperationLogSummaryView> result = operationLogApplicationService.page(
                new OperationLogPageCommand(page, size, sort, startTime, endTime,
                        userId, username, module, action, success));
        return Result.success(new PageResult<>(
                result.items().stream().map(this::toSummary).toList(),
                result.total(), result.page(), result.size()));
    }

    @Operation(operationId = "getOperationLog", summary = "操作日志详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('audit:operation:list')")
    public Result<OperationLogDetail> detail(@PathVariable long id) {
        return Result.success(toDetail(operationLogApplicationService.detail(id)));
    }

    private OperationLogSummary toSummary(OperationLogSummaryView view) {
        return new OperationLogSummary(
                view.id(),
                view.traceId(),
                view.userId(),
                view.username(),
                view.module(),
                view.action(),
                view.requestMethod(),
                view.requestUri(),
                view.success(),
                view.ip(),
                view.durationMs(),
                view.createdAt());
    }

    private OperationLogDetail toDetail(OperationLogDetailView view) {
        return new OperationLogDetail(
                view.id(),
                view.traceId(),
                view.userId(),
                view.username(),
                view.module(),
                view.action(),
                view.method(),
                view.requestMethod(),
                view.requestUri(),
                view.requestParams(),
                view.success(),
                view.errorMessage(),
                view.ip(),
                view.userAgent(),
                view.durationMs(),
                view.createdAt());
    }
}
