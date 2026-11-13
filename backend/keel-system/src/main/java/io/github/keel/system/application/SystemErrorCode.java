package io.github.keel.system.application;

import io.github.keel.common.core.error.ErrorCode;

/**
 * 系统参数模块业务错误码，与 04 API 文档保持一致。
 */
public enum SystemErrorCode implements ErrorCode {

    CONFIG_NOT_FOUND("SYS_CONFIG_NOT_FOUND", 404, "系统参数不存在"),
    CONFIG_VALUE_INVALID("SYS_CONFIG_VALUE_INVALID", 400, "系统参数值不合法"),
    CONFIG_PUBLIC_FORBIDDEN("SYS_CONFIG_PUBLIC_FORBIDDEN", 400, "该参数不允许公开");

    private final String code;
    private final int httpStatus;
    private final String message;

    SystemErrorCode(String code, int httpStatus, String message) {
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
