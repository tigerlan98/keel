package io.github.keel.iam.application.organization;

public record DepartmentNodeView(
        Long id,
        Long parentId,
        String code,
        String name,
        Integer sort,
        DepartmentStatus status,
        Integer version,
        long directUserCount,
        java.util.List<DepartmentNodeView> children) {}
