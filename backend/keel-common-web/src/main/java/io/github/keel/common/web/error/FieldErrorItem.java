package io.github.keel.common.web.error;

/**
 * 字段级校验错误。
 *
 * @param field   字段名
 * @param message 错误提示
 */
public record FieldErrorItem(String field, String message) {
}
