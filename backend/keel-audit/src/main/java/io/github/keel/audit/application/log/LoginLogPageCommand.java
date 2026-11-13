package io.github.keel.audit.application.log;

import io.github.keel.common.core.event.LoginAction;

import java.time.LocalDateTime;

/**
 * 登录日志分页查询条件。
 */
public record LoginLogPageCommand(Integer page, Integer size, String sort, LocalDateTime startTime,
                                  LocalDateTime endTime, Long userId, String username,
                                  LoginAction action, Boolean success, String ip) {
}
