package io.github.keel.audit.application;

import io.github.keel.common.core.error.ErrorCode;

/**
 * 审计模块业务错误码，与 04 API 文档保持一致。
 */
public enum AuditErrorCode implements ErrorCode {

    LOG_NOT_FOUND("AUDIT_LOG_NOT_FOUND", 404, "日志不存在");

    private final String code;
    private final int httpStatus;
    private final String message;

    AuditErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
