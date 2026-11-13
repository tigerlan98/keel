package io.github.keel.audit.application.log;

import io.github.keel.audit.infrastructure.persistence.mapper.OperationLogMapper;
import io.github.keel.audit.infrastructure.persistence.po.OperationLogPO;
import io.github.keel.common.core.event.OperationLogEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志异步消费。事件在业务事务提交后发布，写入失败不影响业务结果。
 */
@Component
@RequiredArgsConstructor
public class OperationLogListener {

    private static final Logger log = LoggerFactory.getLogger(OperationLogListener.class);

    private final OperationLogMapper operationLogMapper;

    @Async("auditExecutor")
    @EventListener
    public void onOperationLog(OperationLogEvent event) {
        try {
            OperationLogPO logPO = new OperationLogPO();
            logPO.setTraceId(event.traceId());
            logPO.setUserId(event.userId());
            logPO.setUsername(event.username());
            logPO.setModule(event.module());
            logPO.setAction(event.action());
            logPO.setMethod(event.method());
            logPO.setRequestMethod(event.requestMethod());
            logPO.setRequestUri(event.requestUri());
            logPO.setRequestParams(event.requestParams());
            logPO.setSuccess(event.success());
            logPO.setErrorMessage(event.errorMessage());
            logPO.setIp(event.ip());
            logPO.setUserAgent(event.userAgent());
            logPO.setDurationMs(event.durationMs());
            operationLogMapper.insert(logPO);
        } catch (RuntimeException exception) {
            log.error("写入操作日志失败: module={}, action={}",
                    event.module(), event.action(), exception);
        }
    }
}
