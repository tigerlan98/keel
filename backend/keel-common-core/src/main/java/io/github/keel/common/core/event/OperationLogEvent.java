package io.github.keel.common.core.event;

/**
 * 操作日志事件，由审计切面发布、审计模块异步消费。
 *
 * @param traceId       请求链路标识
 * @param userId        操作人 ID，匿名或系统任务为空
 * @param username      操作人用户名
 * @param module        模块编码
 * @param action        动作编码
 * @param method        类名#方法名
 * @param requestMethod HTTP 方法
 * @param requestUri    请求 URI
 * @param requestParams 脱敏并截断后的请求参数
 * @param success       是否成功
 * @param errorMessage  错误信息，成功时为空
 * @param ip            来源 IP
 * @param userAgent     User-Agent
 * @param durationMs    耗时毫秒
 */
public record OperationLogEvent(String traceId, Long userId, String username, String module,
                                String action, String method, String requestMethod,
                                String requestUri, String requestParams, boolean success,
                                String errorMessage, String ip, String userAgent, Integer durationMs) {
}
