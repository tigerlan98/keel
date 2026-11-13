package io.github.keel.common.core.config;

import java.util.Optional;

/**
 * 系统参数读取端口。实现位于 keel-system，业务模块只依赖本契约。
 */
public interface ConfigProvider {

    /**
     * 按 key 读取参数值；参数不存在时返回空。
     */
    Optional<String> get(String key);

    /**
     * 按 key 读取参数值；参数不存在或值为空时返回默认值。
     */
    default String getOrDefault(String key, String defaultValue) {
        return get(key).filter(value -> !value.isBlank()).orElse(defaultValue);
    }
}
