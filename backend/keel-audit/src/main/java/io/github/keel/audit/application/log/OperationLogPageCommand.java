package io.github.keel.audit.application.log;

import java.time.LocalDateTime;

/**
 * 操作日志分页查询条件。
 */
public record OperationLogPageCommand(Integer page, Integer size, String sort, LocalDateTime startTime,
                                      LocalDateTime endTime, Long userId, String username,
                                      String module, String action, Boolean success) {
}
