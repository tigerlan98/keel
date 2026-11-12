package io.github.keel.common.core.error;

/**
 * 跨模块通用错误码。
 */
public enum CommonErrorCode implements ErrorCode {

    VALIDATION_FAILED("COMMON_VALIDATION_FAILED", 400, "参数校验失败"),
    BAD_REQUEST("COMMON_BAD_REQUEST", 400, "请求参数错误"),
    UNAUTHORIZED("COMMON_UNAUTHORIZED", 401, "未认证"),
    FORBIDDEN("COMMON_FORBIDDEN", 403, "无权限"),
    NOT_FOUND("COMMON_NOT_FOUND", 404, "资源不存在"),
    METHOD_NOT_ALLOWED("COMMON_METHOD_NOT_ALLOWED", 405, "请求方法不支持"),
    CONFLICT("COMMON_CONFLICT", 409, "资源冲突"),
    OPTIMISTIC_LOCK("COMMON_OPTIMISTIC_LOCK", 409, "数据已被修改，请刷新后重试"),
    PAYLOAD_TOO_LARGE("COMMON_PAYLOAD_TOO_LARGE", 413, "请求体过大"),
    UNSUPPORTED_MEDIA_TYPE("COMMON_UNSUPPORTED_MEDIA_TYPE", 415, "不支持的请求格式"),
    INTERNAL_ERROR("COMMON_INTERNAL_ERROR", 500, "系统内部错误");

    private final String code;
    private final int httpStatus;
    private final String message;

    CommonErrorCode(String code, int httpStatus, String message) {
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
