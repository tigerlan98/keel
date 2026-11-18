package io.github.keel.iam.application;

import io.github.keel.common.core.error.ErrorCode;

/** IAM 模块业务错误码，与 04 API 文档保持一致。 */
public enum IamErrorCode implements ErrorCode {
    DEPARTMENT_NOT_FOUND("IAM_DEPARTMENT_NOT_FOUND", 404, "部门不存在"),
    DEPARTMENT_CODE_EXISTS("IAM_DEPARTMENT_CODE_EXISTS", 409, "部门编码已存在"),
    DEPARTMENT_NAME_EXISTS("IAM_DEPARTMENT_NAME_EXISTS", 409, "同级部门名称已存在"),
    DEPARTMENT_DISABLED("IAM_DEPARTMENT_DISABLED", 409, "部门或上级部门已停用"),
    DEPARTMENT_CYCLE("IAM_DEPARTMENT_CYCLE", 409, "部门不能移动到自己或下级部门"),
    DEPARTMENT_DEPTH_EXCEEDED("IAM_DEPARTMENT_DEPTH_EXCEEDED", 409, "部门层级最多为16层"),
    DEPARTMENT_LIMIT_EXCEEDED("IAM_DEPARTMENT_LIMIT_EXCEEDED", 409, "部门最多为1000个"),
    DEPARTMENT_HAS_ENABLED_DESCENDANTS(
            "IAM_DEPARTMENT_HAS_ENABLED_DESCENDANTS", 409, "请先停用启用中的下级部门"),
    DEPARTMENT_HAS_CHILDREN("IAM_DEPARTMENT_HAS_CHILDREN", 409, "部门存在下级，不能删除"),
    DEPARTMENT_IN_USE("IAM_DEPARTMENT_IN_USE", 409, "部门仍有关联用户，请先调整用户归属"),
    DEPARTMENT_PROTECTED("IAM_DEPARTMENT_PROTECTED", 409, "根部门不允许此操作"),
    USER_NOT_FOUND("IAM_USER_NOT_FOUND", 404, "用户不存在"),
    USER_PROTECTED("IAM_USER_PROTECTED", 409, "当前登录账号或超级管理员账号不可删除"),
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
