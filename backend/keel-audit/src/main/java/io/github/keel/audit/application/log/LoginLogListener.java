package io.github.keel.audit.application.log;

import io.github.keel.audit.infrastructure.persistence.mapper.LoginLogMapper;
import io.github.keel.audit.infrastructure.persistence.po.LoginLogPO;
import io.github.keel.common.core.event.LoginLogEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 登录日志同步消费。审计写入失败不阻断登录流程。
 */
@Component
@RequiredArgsConstructor
public class LoginLogListener {

    private static final Logger log = LoggerFactory.getLogger(LoginLogListener.class);
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final LoginLogMapper loginLogMapper;

    @EventListener
    public void onLoginLog(LoginLogEvent event) {
        try {
            LoginLogPO logPO = new LoginLogPO();
            logPO.setTraceId(MDC.get(REQUEST_ID_MDC_KEY));
            logPO.setUserId(event.userId());
            logPO.setUsername(truncate(event.username(), 64));
            logPO.setAction(event.action().name());
            logPO.setLoginType(event.loginType());
            logPO.setDevice(event.device());
            logPO.setIp(truncate(event.ip(), 64));
            logPO.setUserAgent(truncate(event.userAgent(), 512));
            logPO.setSuccess(event.success());
            logPO.setFailureReason(truncate(event.failureReason(), 64));
            loginLogMapper.insert(logPO);
        } catch (RuntimeException exception) {
            // 不记录密码或 token
            log.error("写入登录日志失败: username={}, action={}",
                    event.username(), event.action(), exception);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
