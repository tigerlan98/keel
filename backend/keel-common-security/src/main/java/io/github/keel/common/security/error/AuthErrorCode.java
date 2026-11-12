package io.github.keel.common.security.error;

import io.github.keel.common.core.error.ErrorCode;

/**
 * 认证相关错误码。
 */
public enum AuthErrorCode implements ErrorCode {

    AUTH_BAD_CREDENTIALS("AUTH_BAD_CREDENTIALS", 401, "用户名或密码错误"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", 401, "登录状态已失效"),
    AUTH_CSRF_INVALID("AUTH_CSRF_INVALID", 403, "CSRF 校验失败"),
    AUTH_PASSWORD_CHANGE_REQUIRED("AUTH_PASSWORD_CHANGE_REQUIRED", 403, "必须先修改密码"),
    AUTH_OLD_PASSWORD_INVALID("AUTH_OLD_PASSWORD_INVALID", 400, "当前密码不正确"),
    AUTH_TOO_MANY_ATTEMPTS("AUTH_TOO_MANY_ATTEMPTS", 429, "登录失败次数过多，请稍后再试");

    private final String code;
    private final int httpStatus;
    private final String message;

    AuthErrorCode(String code, int httpStatus, String message) {
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
