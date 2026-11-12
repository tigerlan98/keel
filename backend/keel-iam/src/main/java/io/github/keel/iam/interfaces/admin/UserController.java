package io.github.keel.iam.interfaces.admin;

import io.github.keel.common.core.result.PageResult;
import io.github.keel.common.core.result.Result;
import io.github.keel.iam.application.identity.UserPageCommand;
import io.github.keel.iam.application.identity.UserQueryApplicationService;
import io.github.keel.iam.application.identity.UserStatus;
import io.github.keel.iam.application.identity.UserSummaryView;
import io.github.keel.iam.interfaces.admin.dto.UserSummary;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户查询接口。
 */
@RestController
@RequestMapping("/api/v1/admin/iam/users")
@Validated
public class UserController {

    private final UserQueryApplicationService userQueryApplicationService;

    public UserController(UserQueryApplicationService userQueryApplicationService) {
        this.userQueryApplicationService = userQueryApplicationService;
    }

    @GetMapping
    @PreAuthorize("@perm.has('iam:user:list')")
    public Result<PageResult<UserSummary>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 64) String sort,
            @RequestParam(required = false) @Size(max = 64) String username,
            @RequestParam(required = false) UserStatus status) {
        PageResult<UserSummaryView> result = userQueryApplicationService.page(
                new UserPageCommand(page, size, sort, username, status));

        return Result.success(new PageResult<>(
                result.items().stream().map(this::toSummary).toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    private UserSummary toSummary(UserSummaryView view) {
        return new UserSummary(
                view.id(),
                view.username(),
                view.nickname(),
                view.status(),
                view.lastLoginAt(),
                view.createdAt());
    }
}
