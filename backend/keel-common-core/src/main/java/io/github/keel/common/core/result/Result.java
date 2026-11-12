package io.github.keel.common.core.result;

/**
 * 统一响应结构。
 *
 * @param code    业务状态码，成功固定为 {@link #SUCCESS_CODE}
 * @param message 提示信息
 * @param data    业务数据
 */
public record Result<T>(String code, String message, T data) {

    public static final String SUCCESS_CODE = "OK";
    public static final String SUCCESS_MESSAGE = "success";

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static Result<Void> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    public static <T> Result<T> failure(String code, String message, T data) {
        return new Result<>(code, message, data);
    }

    public static Result<Void> failure(String code, String message) {
        return new Result<>(code, message, null);
    }
}
