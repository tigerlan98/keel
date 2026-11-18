package io.github.keel.iam.infrastructure.persistence.mapper;

public record UserRoleProjection(Long userId, Long id, String code, String name, String status) {}
