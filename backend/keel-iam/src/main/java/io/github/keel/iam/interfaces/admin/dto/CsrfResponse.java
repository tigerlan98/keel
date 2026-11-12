package io.github.keel.iam.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * CSRF token 响应。
 */
public record CsrfResponse(@Schema(requiredMode = REQUIRED) String token) {
}
