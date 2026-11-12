package io.github.keel.iam.application;

import io.github.keel.common.core.error.ErrorCode;

/**
 * IAM 模块业务错误码，与 04 API 文档保持一致。
 */
public enum IamErrorCode implements ErrorCode {

    USER_NOT_FOUND("IAM_USER_NOT_FOUND", 404, "用户不存在"),
    USERNAME_EXISTS("IAM_USERNAME_EXISTS", 409, "用户名已存在"),
    ROLE_NOT_FOUND("IAM_ROLE_NOT_FOUND", 404, "角色不存在"),
    ROLE_CODE_EXISTS("IAM_ROLE_CODE_EXISTS", 409, "角色编码已存在"),
    ROLE_IN_USE("IAM_ROLE_IN_USE", 409, "角色已被用户引用"),
    ROLE_PROTECTED("IAM_ROLE_PROTECTED", 409, "内置角色受保护，不允许该操作"),
    INVALID_ROLE_IDS("IAM_INVALID_ROLE_IDS", 400, "角色列表包含无效项"),
    RESOURCE_NOT_FOUND("IAM_RESOURCE_NOT_FOUND", 404, "资源不存在"),
    RESOURCE_PARENT_INVALID("IAM_RESOURCE_PARENT_INVALID", 400, "目标父级不存在或会形成环"),
    RESOURCE_HAS_ENABLED_CHILDREN("IAM_RESOURCE_HAS_ENABLED_CHILDREN", 409, "资源存在启用子资源，不能停用"),
    INVALID_RESOURCE_IDS("IAM_INVALID_RESOURCE_IDS", 400, "资源列表包含无效项");

    private final String code;
    private final int httpStatus;
    private final String message;

    IamErrorCode(String code, int httpStatus, String message) {
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
