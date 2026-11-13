package io.github.keel.system.application.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.keel.common.core.auth.CurrentUserProvider;
import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.core.result.PageResult;
import io.github.keel.system.application.SystemErrorCode;
import io.github.keel.system.infrastructure.cache.ConfigCache;
import io.github.keel.system.infrastructure.persistence.mapper.SysConfigMapper;
import io.github.keel.system.infrastructure.persistence.po.SysConfigPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统参数查询、更新与公开配置读取。参数新增/删除仅由 Flyway 迁移维护。
 */
@Service
@RequiredArgsConstructor
public class ConfigApplicationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_VALUE_LENGTH = 4000;
    private static final String PUBLIC_KEY_PREFIX = "system.";
    private static final String SYSTEM_NAME_KEY = "system.name";
    private static final String SYSTEM_LOGO_URL_KEY = "system.logo-url";
    private static final String DEFAULT_SYSTEM_NAME = "Keel";

    private final SysConfigMapper configMapper;
    private final ConfigCache configCache;
    private final CurrentUserProvider currentUserProvider;

    public PageResult<ConfigSummaryView> page(ConfigPageCommand command) {
        int pageNumber = command.page() == null ? DEFAULT_PAGE : command.page();
        int pageSize = command.size() == null ? DEFAULT_SIZE : command.size();

        LambdaQueryWrapper<SysConfigPO> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(command.group())) {
            wrapper.eq(SysConfigPO::getConfigGroup, command.group().trim());
        }
        if (StringUtils.hasText(command.keyword())) {
            wrapper.likeRight(SysConfigPO::getConfigKey, command.keyword().trim());
        }
        applySort(wrapper, command.sort());

        Page<SysConfigPO> result = configMapper.selectPage(new Page<>(pageNumber, pageSize), wrapper);
        List<ConfigSummaryView> items = result.getRecords().stream().map(this::toSummary).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ConfigDetailView detail(long id) {
        return toDetail(requireConfig(id));
    }

    @Transactional
    public ConfigDetailView update(long id, UpdateConfigCommand command) {
        SysConfigPO config = requireConfig(id);
        validateValue(config.getConfigKey(), command.configValue());
        if (Boolean.TRUE.equals(command.publicFlag()) && !config.getConfigKey().startsWith(PUBLIC_KEY_PREFIX)) {
            throw new BusinessException(SystemErrorCode.CONFIG_PUBLIC_FORBIDDEN);
        }

        // 显式 set(null) 以支持清空 description；同时按 version 做乐观锁校验
        int updated = configMapper.update(null, Wrappers.<SysConfigPO>lambdaUpdate()
                .eq(SysConfigPO::getId, id)
                .eq(SysConfigPO::getVersion, command.version())
                .set(SysConfigPO::getConfigValue, command.configValue())
                .set(SysConfigPO::getDescription, command.description())
                .set(SysConfigPO::getPublicFlag, Boolean.TRUE.equals(command.publicFlag()))
                .set(SysConfigPO::getVersion, command.version() + 1)
                .set(SysConfigPO::getUpdatedAt, LocalDateTime.now())
                .set(SysConfigPO::getUpdatedBy, currentUserId()));
        if (updated == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK);
        }

        configCache.evict(config.getConfigKey());
        return toDetail(configMapper.selectById(id));
    }

    public PublicSiteConfigView publicSiteConfig() {
        Map<String, String> publicValues = configMapper.selectList(Wrappers.<SysConfigPO>lambdaQuery()
                        .eq(SysConfigPO::getPublicFlag, true)
                        .likeRight(SysConfigPO::getConfigKey, PUBLIC_KEY_PREFIX))
                .stream()
                .collect(Collectors.toMap(SysConfigPO::getConfigKey, SysConfigPO::getConfigValue));
        return new PublicSiteConfigView(
                publicValues.getOrDefault(SYSTEM_NAME_KEY, DEFAULT_SYSTEM_NAME),
                publicValues.getOrDefault(SYSTEM_LOGO_URL_KEY, ""));
    }

    private SysConfigPO requireConfig(long id) {
        SysConfigPO config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(SystemErrorCode.CONFIG_NOT_FOUND);
        }
        return config;
    }

    private Long currentUserId() {
        Long userId = currentUserProvider.currentUserId();
        return userId == null ? 0L : userId;
    }

    private void validateValue(String key, String value) {
        if (value == null || value.length() > MAX_VALUE_LENGTH) {
            throw new BusinessException(SystemErrorCode.CONFIG_VALUE_INVALID,
                    "参数值不能为空且长度不能超过 4000");
        }

        switch (key) {
            case SYSTEM_NAME_KEY -> requireTextLength(value, 1, 64, "系统名称长度必须在 1 到 64 之间");
            case SYSTEM_LOGO_URL_KEY -> requireTextLength(value, 0, 512, "Logo 地址长度不能超过 512");
            case "auth.web.idle-timeout-minutes" -> requireInteger(value, 1, 1440, "Web 空闲超时必须在 1 到 1440 分钟之间");
            case "auth.web.absolute-timeout-hours" -> requireInteger(value, 1, 720, "Web 绝对超时必须在 1 到 720 小时之间");
            case "auth.app.idle-timeout-days" -> requireInteger(value, 1, 30, "App 空闲超时必须在 1 到 30 天之间");
            case "auth.app.absolute-timeout-days" -> requireInteger(value, 1, 365, "App 绝对超时必须在 1 到 365 天之间");
            case "auth.max-login-failures" -> requireInteger(value, 1, 100, "登录失败上限必须在 1 到 100 之间");
            case "auth.lock-minutes" -> requireInteger(value, 1, 1440, "登录锁定时长必须在 1 到 1440 分钟之间");
            case "audit.operation.retention-days" -> requireInteger(value, 1, 3650, "操作日志保留天数必须在 1 到 3650 之间");
            case "audit.login.retention-days" -> requireInteger(value, 1, 3650, "登录日志保留天数必须在 1 到 3650 之间");
            default -> {
                // 迁移新增的参数只做通用长度校验
            }
        }
    }

    private void requireTextLength(String value, int min, int max, String message) {
        int length = value.length();
        if (length < min || length > max) {
            throw new BusinessException(SystemErrorCode.CONFIG_VALUE_INVALID, message);
        }
    }

    private void requireInteger(String value, int min, int max, String message) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new BusinessException(SystemErrorCode.CONFIG_VALUE_INVALID, message);
            }
        } catch (NumberFormatException exception) {
            throw new BusinessException(SystemErrorCode.CONFIG_VALUE_INVALID, message);
        }
    }

    private void applySort(LambdaQueryWrapper<SysConfigPO> wrapper, String sort) {
        String field = "updatedAt";
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
            case "configKey" -> wrapper.orderBy(true, ascending, SysConfigPO::getConfigKey);
            case "updatedAt" -> wrapper.orderBy(true, ascending, SysConfigPO::getUpdatedAt);
            default -> throw new BusinessException(CommonErrorCode.BAD_REQUEST, "排序字段不支持");
        }
    }

    private ConfigSummaryView toSummary(SysConfigPO config) {
        return new ConfigSummaryView(
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getConfigGroup(),
                config.getPublicFlag(),
                config.getDescription(),
                config.getUpdatedAt(),
                config.getVersion());
    }

    private ConfigDetailView toDetail(SysConfigPO config) {
        return new ConfigDetailView(
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getConfigGroup(),
                config.getPublicFlag(),
                config.getDescription(),
                config.getCreatedAt(),
                config.getCreatedBy(),
                config.getUpdatedAt(),
                config.getUpdatedBy(),
                config.getVersion());
    }
}
