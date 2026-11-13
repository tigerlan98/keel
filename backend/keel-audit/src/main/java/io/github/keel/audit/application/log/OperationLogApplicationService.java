package io.github.keel.audit.application.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.keel.audit.application.AuditErrorCode;
import io.github.keel.audit.infrastructure.persistence.mapper.OperationLogMapper;
import io.github.keel.audit.infrastructure.persistence.po.OperationLogPO;
import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.core.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 操作日志分页查询。
 */
@Service
@RequiredArgsConstructor
public class OperationLogApplicationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final OperationLogMapper operationLogMapper;

    public PageResult<OperationLogSummaryView> page(OperationLogPageCommand command) {
        int pageNumber = command.page() == null ? DEFAULT_PAGE : command.page();
        int pageSize = command.size() == null ? DEFAULT_SIZE : command.size();

        LambdaQueryWrapper<OperationLogPO> wrapper = Wrappers.lambdaQuery();
        if (command.startTime() != null) {
            wrapper.ge(OperationLogPO::getCreatedAt, command.startTime());
        }
        if (command.endTime() != null) {
            wrapper.le(OperationLogPO::getCreatedAt, command.endTime());
        }
        if (command.userId() != null) {
            wrapper.eq(OperationLogPO::getUserId, command.userId());
        }
        if (StringUtils.hasText(command.username())) {
            wrapper.likeRight(OperationLogPO::getUsername, command.username().trim());
        }
        if (StringUtils.hasText(command.module())) {
            wrapper.eq(OperationLogPO::getModule, command.module().trim());
        }
        if (StringUtils.hasText(command.action())) {
            wrapper.eq(OperationLogPO::getAction, command.action().trim());
        }
        if (command.success() != null) {
            wrapper.eq(OperationLogPO::getSuccess, command.success());
        }
        applySort(wrapper, command.sort());

        Page<OperationLogPO> result = operationLogMapper.selectPage(new Page<>(pageNumber, pageSize), wrapper);
        List<OperationLogSummaryView> items = result.getRecords().stream().map(this::toSummary).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public OperationLogDetailView detail(long id) {
        OperationLogPO logPO = operationLogMapper.selectById(id);
        if (logPO == null) {
            throw new BusinessException(AuditErrorCode.LOG_NOT_FOUND);
        }
        return new OperationLogDetailView(
                logPO.getId(),
                logPO.getTraceId(),
                logPO.getUserId(),
                logPO.getUsername(),
                logPO.getModule(),
                logPO.getAction(),
                logPO.getMethod(),
                logPO.getRequestMethod(),
                logPO.getRequestUri(),
                logPO.getRequestParams(),
                logPO.getSuccess(),
                logPO.getErrorMessage(),
                logPO.getIp(),
                logPO.getUserAgent(),
                logPO.getDurationMs(),
                logPO.getCreatedAt());
    }

    private void applySort(LambdaQueryWrapper<OperationLogPO> wrapper, String sort) {
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

        wrapper.orderBy(true, ascending, OperationLogPO::getCreatedAt);
    }

    private OperationLogSummaryView toSummary(OperationLogPO logPO) {
        return new OperationLogSummaryView(
                logPO.getId(),
                logPO.getTraceId(),
                logPO.getUserId(),
                logPO.getUsername(),
                logPO.getModule(),
                logPO.getAction(),
                logPO.getRequestMethod(),
                logPO.getRequestUri(),
                logPO.getSuccess(),
                logPO.getIp(),
                logPO.getDurationMs(),
                logPO.getCreatedAt());
    }
}
