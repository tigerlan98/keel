package io.github.keel.iam.application.identity;

public record UpdateUserProfileCommand(
        String nickname, String email, String phone, Long departmentId, Integer version) {}
