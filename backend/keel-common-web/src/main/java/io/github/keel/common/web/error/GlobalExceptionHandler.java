package io.github.keel.common.web.error;

import io.github.keel.common.core.error.CommonErrorCode;
import io.github.keel.common.core.error.ErrorCode;
import io.github.keel.common.core.exception.BusinessException;
import io.github.keel.common.core.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 全局异常处理，保证所有错误响应结构与错误码一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("业务异常: code={}, message={}", errorCode.code(), exception.getMessage());
        return ResponseEntity.status(errorCode.httpStatus())
                .body(Result.failure(errorCode.code(), exception.getMessage(), exception.getData()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<ValidationErrorData>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        List<FieldErrorItem> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorItem(error.getField(), error.getDefaultMessage()))
                .toList();
        return validationFailure(fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<ValidationErrorData>> handleConstraintViolation(
            ConstraintViolationException exception) {
        List<FieldErrorItem> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new FieldErrorItem(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        return validationFailure(fieldErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception exception) {
        log.warn("请求参数错误: {}", exception.getMessage());
        return failure(CommonErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoResourceFoundException exception) {
        return failure(CommonErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return failure(CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return failure(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        // 仅记录在服务端日志，响应中不暴露实现细节
        log.error("未预期的系统异常", exception);
        return failure(CommonErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<Result<ValidationErrorData>> validationFailure(List<FieldErrorItem> fieldErrors) {
        CommonErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(errorCode.httpStatus())
                .body(Result.failure(errorCode.code(), errorCode.message(), new ValidationErrorData(fieldErrors)));
    }

    private ResponseEntity<Result<Void>> failure(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.httpStatus())
                .body(Result.failure(errorCode.code(), errorCode.message()));
    }
}
