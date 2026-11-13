package io.github.keel.system.application.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.keel.common.core.config.ConfigProvider;
import io.github.keel.system.infrastructure.cache.ConfigCache;
import io.github.keel.system.infrastructure.persistence.mapper.SysConfigMapper;
import io.github.keel.system.infrastructure.persistence.po.SysConfigPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 系统参数读取：优先 Redis 缓存，未命中时回源数据库。
 */
@Component
@RequiredArgsConstructor
public class SystemConfigProvider implements ConfigProvider {

    private final SysConfigMapper configMapper;
    private final ConfigCache configCache;

    @Override
    public Optional<String> get(String key) {
        return configCache.get(key).or(() -> {
            SysConfigPO config = configMapper.selectOne(Wrappers.<SysConfigPO>lambdaQuery()
                    .eq(SysConfigPO::getConfigKey, key));
            if (config == null) {
                return Optional.empty();
            }
            configCache.put(key, config.getConfigValue());
            return Optional.of(config.getConfigValue());
        });
    }
}
