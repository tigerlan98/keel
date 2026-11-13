package io.github.keel.system.application.config;

/**
 * 更新系统参数命令。
 */
public record UpdateConfigCommand(String configValue, String description, Boolean publicFlag, Integer version) {
}
