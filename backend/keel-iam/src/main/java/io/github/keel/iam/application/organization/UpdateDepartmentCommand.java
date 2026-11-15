package io.github.keel.iam.application.organization;

public record UpdateDepartmentCommand(Long parentId, String name, Integer sort, Integer version) {}
