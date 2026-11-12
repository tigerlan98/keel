package io.github.keel.common.core.error;

/**
 * 业务错误码契约。错误码一经发布不得复用或改变语义。
 */
public interface ErrorCode {

    /**
     * 稳定、可识别的错误码，例如 {@code COMMON_VALIDATION_FAILED}。
     */
    String code();

    /**
     * 该错误码对应的 HTTP 状态码。
     */
    int httpStatus();

    /**
     * 面向调用方的默认提示信息。
     */
    String message();
}
