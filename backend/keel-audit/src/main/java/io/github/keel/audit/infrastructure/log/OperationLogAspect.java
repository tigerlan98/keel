package io.github.keel.audit.infrastructure.log;

import io.github.keel.common.core.audit.OperationLog;
import io.github.keel.common.core.event.OperationLogEvent;
import io.github.keel.common.security.auth.AuthenticatedUser;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 操作日志切面：方法成功返回后发布事件，由审计监听器异步落库。
 *
 * <p>方法抛异常时事务回滚，不产生操作日志；请求参数中的密码、token 等字段会被脱敏。</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int MAX_PARAMS_LENGTH = 2048;
    private static final String MASK = "***";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.nanoTime();
        Object result = joinPoint.proceed();
        int durationMs = (int) Math.min((System.nanoTime() - start) / 1_000_000, Integer.MAX_VALUE);
        publish(joinPoint, operationLog, durationMs);
        return result;
    }

    private void publish(ProceedingJoinPoint joinPoint, OperationLog operationLog, int durationMs) {
        try {
            HttpServletRequest request = currentRequest();
            AuthenticatedUser user = currentUser();
            eventPublisher.publishEvent(new OperationLogEvent(
                    MDC.get(REQUEST_ID_MDC_KEY),
                    user == null ? null : user.id(),
                    user == null ? null : user.username(),
                    operationLog.module(),
                    operationLog.action(),
                    joinPoint.getSignature().getDeclaringType().getSimpleName() + "#"
                            + joinPoint.getSignature().getName(),
                    request == null ? null : request.getMethod(),
                    request == null ? null : request.getRequestURI(),
                    serializeParams(joinPoint.getArgs()),
                    true,
                    null,
                    request == null ? null : request.getRemoteAddr(),
                    request == null ? null : request.getHeader(HttpHeaders.USER_AGENT),
                    durationMs));
        } catch (RuntimeException exception) {
            // 审计失败不影响业务结果
            log.error("发布操作日志事件失败: module={}, action={}",
                    operationLog.module(), operationLog.action(), exception);
        }
    }

    private String serializeParams(Object[] args) {
        List<Object> params = new ArrayList<>();
        if (args != null) {
            Arrays.stream(args).filter(this::isBusinessParam).forEach(params::add);
        }
        if (params.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.valueToTree(params);
            maskSensitive(node);
            String json = objectMapper.writeValueAsString(node);
            return json.length() <= MAX_PARAMS_LENGTH ? json : json.substring(0, MAX_PARAMS_LENGTH);
        } catch (Exception exception) {
            log.debug("序列化操作日志参数失败", exception);
            return null;
        }
    }

    private boolean isBusinessParam(Object value) {
        return value != null
                && !(value instanceof ServletRequest)
                && !(value instanceof ServletResponse)
                && !(value instanceof Authentication)
                && !(value instanceof Principal);
    }

    private void maskSensitive(JsonNode node) {
        if (node.isObject()) {
            List<String> sensitiveKeys = new ArrayList<>();
            for (var entry : node.properties()) {
                if (isSensitiveKey(entry.getKey())) {
                    sensitiveKeys.add(entry.getKey());
                } else {
                    maskSensitive(entry.getValue());
                }
            }
            sensitiveKeys.forEach(key -> ((ObjectNode) node).put(key, MASK));
        } else if (node.isArray()) {
            node.forEach(this::maskSensitive);
        }
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("secret")
                || "authorization".equals(normalized)
                || "cookie".equals(normalized);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletAttributes
                ? servletAttributes.getRequest()
                : null;
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                ? user
                : null;
    }
}
