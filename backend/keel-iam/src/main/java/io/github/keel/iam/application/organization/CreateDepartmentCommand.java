package io.github.keel.iam.application.organization;

public record CreateDepartmentCommand(
        Long parentId, String code, String name, Integer sort, DepartmentStatus status) {}
