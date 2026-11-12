package io.github.keel.common.web.error;

import java.util.List;

/**
 * 参数校验失败时放在 {@code data} 中的错误明细。
 */
public record ValidationErrorData(List<FieldErrorItem> fieldErrors) {
}
