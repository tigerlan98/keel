package io.github.keel.iam.interfaces.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

public record UpdateUserProfileRequest(
        @NotBlank @Size(max = 64) String nickname,
        @JsonProperty(value = "email", required = true)
                @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
                @Email
                @Size(max = 128)
                String email,
        @JsonProperty(value = "phone", required = true)
                @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
                @Pattern(regexp = "^[0-9+-]{6,32}$")
                String phone,
        @NotNull @Min(1) Long departmentId,
        @NotNull @Min(0) Integer version) {}
