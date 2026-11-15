package io.github.keel.iam.application.organization;

public record DepartmentDetailView(
        Long id,
        Long parentId,
        String code,
        String name,
        Integer sort,
        DepartmentStatus status,
        Integer version,
        long directUserCount,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {}
