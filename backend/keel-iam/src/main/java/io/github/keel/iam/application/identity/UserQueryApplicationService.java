package io.github.keel.iam.application.identity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.core.result.PageResult;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/** 用户分页查询。 */
@Service
@RequiredArgsConstructor
public class UserQueryApplicationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final IamUserMapper userMapper;
    private final io.github.keel.iam.application.organization.DepartmentApplicationService
            departments;
    private final io.github.keel.iam.application.authorization.UserRoleQueryService roles;

    public PageResult<UserSummaryView> page(UserPageCommand command) {
        int pageNumber = command.page() == null ? DEFAULT_PAGE : command.page();
        int pageSize = command.size() == null ? DEFAULT_SIZE : command.size();

        LambdaQueryWrapper<IamUserPO> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(command.username())) {
            wrapper.likeRight(IamUserPO::getUsername, command.username().trim());
        }
        if (command.status() != null) {
            wrapper.eq(IamUserPO::getStatus, command.status().name());
        }
        if (command.unassignedDepartment() && command.departmentId() != null
                || command.includeDescendants() != null && command.departmentId() == null)
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
        if (command.unassignedDepartment()) wrapper.isNull(IamUserPO::getDepartmentId);
        if (command.departmentId() != null)
            wrapper.in(
                    IamUserPO::getDepartmentId,
                    departments.filterIds(
                            command.departmentId(),
                            !Boolean.FALSE.equals(command.includeDescendants())));
        if (command.keyword() != null) {
            if (command.keyword().isBlank())
                throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
            String prefix = escapePrefix(command.keyword().trim());
            wrapper.and(
                    q ->
                            q.likeRight(IamUserPO::getUsername, prefix)
                                    .or()
                                    .likeRight(IamUserPO::getNickname, prefix)
                                    .or()
                                    .likeRight(IamUserPO::getPhone, prefix));
        }
        applySort(wrapper, command.sort());

        Page<IamUserPO> result = userMapper.selectPage(new Page<>(pageNumber, pageSize), wrapper);
        var departmentIds =
                result.getRecords().stream()
                        .map(IamUserPO::getDepartmentId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
        var departmentNames = departments.briefs(departmentIds);
        var userRoles = roles.rolesOf(result.getRecords().stream().map(IamUserPO::getId).toList());
        List<UserSummaryView> items =
                result.getRecords().stream()
                        .map(
                                user -> {
                                    var department =
                                            user.getDepartmentId() == null
                                                    ? null
                                                    : departmentNames.get(user.getDepartmentId());
                                    return new UserSummaryView(
                                            user.getId(),
                                            user.getUsername(),
                                            user.getNickname(),
                                            user.getStatus(),
                                            user.getLastLoginAt(),
                                            user.getCreatedAt(),
                                            user.getPhone(),
                                            user.getDepartmentId(),
                                            department == null ? null : department.name(),
                                            userRoles.getOrDefault(user.getId(), List.of()));
                                })
                        .toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private void applySort(LambdaQueryWrapper<IamUserPO> wrapper, String sort) {
        String field = "createdAt";
        boolean ascending = false;

        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",", 2);
            field = parts[0].trim();
            if (parts.length > 1) {
                String direction = parts[1].trim();
                if ("asc".equalsIgnoreCase(direction)) {
                    ascending = true;
                } else if (!"desc".equalsIgnoreCase(direction)) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, "排序方向不支持");
                }
            }
        }

        switch (field) {
            case "createdAt" -> wrapper.orderBy(true, ascending, IamUserPO::getCreatedAt);
            case "username" -> wrapper.orderBy(true, ascending, IamUserPO::getUsername);
            case "lastLoginAt" -> wrapper.orderBy(true, ascending, IamUserPO::getLastLoginAt);
            default -> throw new BusinessException(CommonErrorCode.BAD_REQUEST, "排序字段不支持");
        }
        wrapper.orderBy(true, ascending, IamUserPO::getId);
    }

    private String escapePrefix(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
