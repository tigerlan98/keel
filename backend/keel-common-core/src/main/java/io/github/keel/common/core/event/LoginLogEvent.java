package io.github.keel.common.core.event;

/**
 * 登录日志事件，由 IAM 发布、审计模块同步消费。
 *
 * @param userId        用户 ID，未知用户为 null
 * @param username      登录名
 * @param action        动作
 * @param loginType     账号体系：ADMIN / APP / MINI
 * @param device        终端：WEB / APP / MINI
 * @param ip            来源 IP
 * @param userAgent     User-Agent
 * @param success       是否成功
 * @param failureReason 失败原因编码，成功时为 null
 */
public record LoginLogEvent(Long userId, String username, LoginAction action, String loginType,
                            String device, String ip, String userAgent, boolean success,
                            String failureReason) {
}
