package io.github.keel.system.application.config;

/**
 * 系统参数分页查询条件。
 */
public record ConfigPageCommand(Integer page, Integer size, String sort, String group, String keyword) {
}
