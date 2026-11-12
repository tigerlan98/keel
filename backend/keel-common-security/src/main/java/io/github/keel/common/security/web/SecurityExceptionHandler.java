package io.github.keel.common.security.web;

import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.error.ErrorCode;
import io.github.keel.common.core.result.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 方法级鉴权失败统一返回 403，避免被通用异常处理转换为 500。
 *
 * <p>优先级高于 common-web 的全局异常处理。</p>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException exception) {
        ErrorCode errorCode = CommonErrorCode.FORBIDDEN;
        return ResponseEntity.status(errorCode.httpStatus())
                .body(Result.failure(errorCode.code(), errorCode.message()));
    }
}
