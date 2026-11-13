package io.github.keel.audit.application.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.keel.audit.application.AuditErrorCode;
import io.github.keel.audit.infrastructure.persistence.mapper.LoginLogMapper;
import io.github.keel.audit.infrastructure.persistence.po.LoginLogPO;
import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.core.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 登录日志分页查询。
 */
@Service
@RequiredArgsConstructor
public class LoginLogApplicationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final LoginLogMapper loginLogMapper;

    public PageResult<LoginLogSummaryView> page(LoginLogPageCommand command) {
        int pageNumber = command.page() == null ? DEFAULT_PAGE : command.page();
        int pageSize = command.size() == null ? DEFAULT_SIZE : command.size();

        LambdaQueryWrapper<LoginLogPO> wrapper = Wrappers.lambdaQuery();
        if (command.startTime() != null) {
            wrapper.ge(LoginLogPO::getCreatedAt, command.startTime());
        }
        if (command.endTime() != null) {
            wrapper.le(LoginLogPO::getCreatedAt, command.endTime());
        }
        if (command.userId() != null) {
            wrapper.eq(LoginLogPO::getUserId, command.userId());
        }
        if (StringUtils.hasText(command.username())) {
            wrapper.likeRight(LoginLogPO::getUsername, command.username().trim());
        }
        if (command.action() != null) {
            wrapper.eq(LoginLogPO::getAction, command.action().name());
        }
        if (command.success() != null) {
            wrapper.eq(LoginLogPO::getSuccess, command.success());
        }
        if (StringUtils.hasText(command.ip())) {
            wrapper.eq(LoginLogPO::getIp, command.ip().trim());
        }
        applySort(wrapper, command.sort());

        Page<LoginLogPO> result = loginLogMapper.selectPage(new Page<>(pageNumber, pageSize), wrapper);
        List<LoginLogSummaryView> items = result.getRecords().stream().map(this::toSummary).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public LoginLogDetailView detail(long id) {
        LoginLogPO logPO = loginLogMapper.selectById(id);
        if (logPO == null) {
            throw new BusinessException(AuditErrorCode.LOG_NOT_FOUND);
        }
        return new LoginLogDetailView(
                logPO.getId(),
                logPO.getTraceId(),
                logPO.getUserId(),
                logPO.getUsername(),
                logPO.getAction(),
                logPO.getLoginType(),
                logPO.getDevice(),
                logPO.getIp(),
                logPO.getUserAgent(),
                logPO.getSuccess(),
                logPO.getFailureReason(),
                logPO.getCreatedAt());
    }

    private void applySort(LambdaQueryWrapper<LoginLogPO> wrapper, String sort) {
        boolean ascending = false;

        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",", 2);
            String field = parts[0].trim();
            if (!"createdAt".equals(field)) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "排序字段不支持");
            }
            if (parts.length > 1) {
                String direction = parts[1].trim();
                if ("asc".equalsIgnoreCase(direction)) {
                    ascending = true;
                } else if (!"desc".equalsIgnoreCase(direction)) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, "排序方向不支持");
                }
            }
        }

        wrapper.orderBy(true, ascending, LoginLogPO::getCreatedAt);
    }

    private LoginLogSummaryView toSummary(LoginLogPO logPO) {
        return new LoginLogSummaryView(
                logPO.getId(),
                logPO.getTraceId(),
                logPO.getUserId(),
                logPO.getUsername(),
                logPO.getAction(),
                logPO.getLoginType(),
                logPO.getDevice(),
                logPO.getIp(),
                logPO.getSuccess(),
                logPO.getFailureReason(),
                logPO.getCreatedAt());
    }
}
