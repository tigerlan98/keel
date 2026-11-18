package io.github.keel.iam.application.organization;

public record DepartmentOptionView(
        Long id,
        Long parentId,
        String name,
        DepartmentStatus status,
        boolean selectable,
        java.util.List<DepartmentOptionView> children) {}
