package io.github.keel.iam.application.identity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.core.result.PageResult;
import io.github.keel.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.github.keel.iam.infrastructure.persistence.po.IamUserPO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户分页查询。
 */
@Service
public class UserQueryApplicationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final IamUserMapper userMapper;

    public UserQueryApplicationService(IamUserMapper userMapper) {
        this.userMapper = userMapper;
    }

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
        applySort(wrapper, command.sort());

        Page<IamUserPO> result = userMapper.selectPage(new Page<>(pageNumber, pageSize), wrapper);
        List<UserSummaryView> items = result.getRecords().stream().map(this::toView).toList();
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
    }

    private UserSummaryView toView(IamUserPO user) {
        return new UserSummaryView(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
